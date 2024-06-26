package de.meetwithfriends.security.jaas;

import de.meetwithfriends.security.jaas.principal.RolePrincipal;
import de.meetwithfriends.security.jaas.principal.UserPrincipal;
import de.meetwithfriends.security.jdbc.AuthenticationDao;
import de.meetwithfriends.security.jdbc.JdbcAuthenticationService;
import de.meetwithfriends.security.jdbc.data.ConfigurationData;
import de.meetwithfriends.security.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.*;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

public class SaltedHashLoginModule implements LoginModule
{
    private static final Logger LOG = LoggerFactory.getLogger(SaltedHashLoginModule.class);
    private static final String ROLE_PRINCIPAL_CLASS_OPTION = "role-principal-class";

    private Subject subject;
    private CallbackHandler callbackHandler;
    private Map<String, ?> options;
    private boolean debug = false;

    private AuthenticationDao authenticationDao;

    private UserPrincipal userPrincipal;

    private String username;
    private char[] password;

    private boolean succeeded = false;
    private boolean commitSucceeded = false;
    private String rolePrincipalClass;

    public static void main(String[] args) throws Exception {
        if (null == args || args.length == 0 || args[0].length() == 0) {
            LOG.error("need a password arg");
            throw new IllegalArgumentException("Need a password arg");
        }

        String salt = StringUtil.getRandomHexString(args.length > 1 ? Integer.parseInt(args[1]) : 32);
        String password = JdbcAuthenticationService.getSaltedPasswordDigest(args[0], salt, "SHA-256");
        System.out.println(" Password -> " + password);
        System.out.println(" Salt -> " + salt);
    }

    @Override
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map<String, ?> sharedState, Map<String, ?> options)
    {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
        this.options = options;
        this.rolePrincipalClass = (String) options.get(ROLE_PRINCIPAL_CLASS_OPTION);

        initDebugging();
        initAuthenticationDao();
    }

    @Override
    public boolean login() throws LoginException
    {
        if (debug)
        {
            LOG.debug("login...");
        }

        if (callbackHandler == null)
        {
            LOG.error("Error: no CallbackHandler available");
            throw new LoginException("Error: no CallbackHandler available");
        }

        Callback[] callbacks = new Callback[2];
        callbacks[0] = new NameCallback("username");
        callbacks[1] = new PasswordCallback("password", false);

        succeeded = requestCredentialsAndAuthenticate(callbacks);

        if (!succeeded)
        {
            LOG.info("login failed");
            throw new FailedLoginException("login failed");
        }

        if (debug)
        {
            LOG.debug("authentication succeeded");
        }

        return succeeded;
    }

    @Override
    public boolean commit() throws LoginException
    {
        if (debug)
        {
            LOG.debug("committing authentication");
        }

        if (!succeeded)
        {
            return false;
        }

        userPrincipal = new UserPrincipal(username);
        addNonExistentPrincipal(userPrincipal);

        List<String> roles = authenticationDao.loadRoles(username);
        for (String role : roles)
        {
            Principal rolePrincipal = createRole(role);
            addNonExistentPrincipal(rolePrincipal);
        }

        username = null;
        commitSucceeded = true;

        return true;
    }

    private Principal createRole(String roleName) {
        if (rolePrincipalClass != null && rolePrincipalClass.length() > 0) {
            try {
                Class<? extends Principal> clazz = (Class<? extends Principal>) Class.forName(rolePrincipalClass);
                Constructor<? extends Principal> clazzDeclaredConstructor = clazz.getDeclaredConstructor(String.class);
                return clazzDeclaredConstructor.newInstance(roleName);
            } catch (Exception e) {
                LOG.warn("Unable to create instance of class {}, error is {}", rolePrincipalClass, e.getMessage());
            }
        }

        return new RolePrincipal(roleName);
    }

    @Override
    public boolean abort() throws LoginException
    {
        if (debug)
        {
            LOG.debug("aborting authentication");
        }

        if (!succeeded)
        {
            return false;
        }

        if (!commitSucceeded)
        {
            resetData();
            succeeded = false;
        }
        else
        {
            logout();
        }

        return true;
    }

    @Override
    public boolean logout() throws LoginException
    {
        if (debug)
        {
            LOG.debug("Removing principal");
        }

        subject.getPrincipals().remove(userPrincipal);
        resetData();

        succeeded = false;
        succeeded = commitSucceeded;

        return true;
    }

    private void initDebugging()
    {
        String debugOption = (String) options.get("debug");
        if ("true".equalsIgnoreCase(debugOption))
        {
            debug = true;
        }
    }

    private void initAuthenticationDao()
    {
        ConfigurationData configurationData = loadConfigurationData();
        authenticationDao = new AuthenticationDao(configurationData);
    }

    private boolean requestCredentialsAndAuthenticate(Callback[] callbacks) throws LoginException
    {
        boolean authenticated = false;

        try
        {
            callbackHandler.handle(callbacks);

            username = ((NameCallback) callbacks[0]).getName();
            password = loadPassword((PasswordCallback) callbacks[1]);
            if (username == null || password.length == 0)
            {
                LOG.error("Callback handler does not return login data properly");
                throw new LoginException("Callback handler does not return login data properly");
            }

            JdbcAuthenticationService authService = initAuthenticationService();
            authenticated = authService.authenticate(username, new String(password));
        }
        catch (IOException ex)
        {
            LOG.error("Error during user login", ex);
            throw new LoginException(ex.toString());
        }
        catch (UnsupportedCallbackException ex)
        {
            String msg = MessageFormat.format("{0} not available to garner authentication information from the user", ex
                    .getCallback().toString());

            LOG.error(msg);
            throw new LoginException("Error: " + msg);
        }

        return authenticated;
    }

    private char[] loadPassword(PasswordCallback pwCallback)
    {
        char[] tmpPassword = pwCallback.getPassword();
        pwCallback.clearPassword();

        if (tmpPassword == null)
        {
            tmpPassword = new char[0];
        }

        return copyPassword(tmpPassword);
    }

    private char[] copyPassword(char[] tmpPassword)
    {
        char[] copiedPassword = new char[tmpPassword.length];
        System.arraycopy(tmpPassword, 0, copiedPassword, 0, tmpPassword.length);

        return copiedPassword;
    }

    private JdbcAuthenticationService initAuthenticationService()
    {
        String mdAlgorithm = (String) options.get("mdAlgorithm");

        JdbcAuthenticationService authService = new JdbcAuthenticationService(authenticationDao, debug);
        if (mdAlgorithm != null)
        {
            authService.setMdAlgorithm(mdAlgorithm);
        }

        return authService;
    }

    private ConfigurationData loadConfigurationData()
    {
        String dBUser = (String) options.get("dbUser");
        String dBPassword = (String) options.get("dbPassword");
        String dBUrl = (String) options.get("dbURL");
        String dBDriver = (String) options.get("dbDriver");
        String userQuery = (String) options.get("userQuery");
        String roleQuery = (String) options.get("roleQuery");

        ConfigurationData configurationData = new ConfigurationData();
        configurationData.setDbUser(dBUser);
        configurationData.setDbPassword(dBPassword);
        configurationData.setDbUrl(dBUrl);
        configurationData.setDbDriver(dBDriver);
        configurationData.setUserQuery(userQuery);
        configurationData.setRoleQuery(roleQuery);

        return configurationData;

    }

    private void addNonExistentPrincipal(Principal principal)
    {
        if (!subject.getPrincipals().contains(principal))
        {
            if (debug)
            {
                LOG.debug("Adding principal: " + principal);
            }

            subject.getPrincipals().add(principal);
        }
    }

    private void resetData()
    {
        userPrincipal = null;
        username = null;

        if (password != null)
        {
            for (int i = 0; i < password.length; i++)
            {
                password[i] = ' ';
                password = null;
            }
        }
    }
}
