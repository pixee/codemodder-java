package io.codemodder.codemods;

import io.codemodder.CodeChanger;
import io.codemodder.Runner;
import java.util.List;

/**
 * Give an ability for users to list all the codemods so they don't have to reference them
 * individually.
 */
public final class DefaultCodemods {

  /** Get a list of all the codemods in our default set. */
  public static List<Class<? extends CodeChanger>> asList() {
    return List.of(
        AddClarifyingBracesCodemod.class,
        DisableAutomaticDirContextDeserializationCodemod.class,
        HardenJavaDeserializationCodemod.class,
        HardenProcessCreationCodemod.class,
        HardenXMLDecoderCodemod.class,
        HardenXMLInputFactoryCodemod.class,
        HardenXStreamCodemod.class,
        HardenZipEntryPathsCodemod.class,
        HQLParameterizationCodemod.class,
        InputResourceLeakCodemod.class,
        InsecureCookieCodemod.class,
        JDBCResourceLeakCodemod.class,
        JEXLInjectionCodemod.class,
        JSPScriptletXSSCodemod.class,
        LimitReadlineCodemod.class,
        MavenSecureURLCodemod.class,
        OutputResourceLeakCodemod.class,
        PreventFileWriterLeakWithFilesCodemod.class,
        RandomizeSeedCodemod.class,
        SanitizeApacheMultipartFilenameCodemod.class,
        SanitizeHttpHeaderCodemod.class,
        SanitizeSpringMultipartFilenameCodemod.class,
        SecureRandomCodemod.class,
        SQLParameterizerCodemod.class,
        SSRFCodemod.class,
        StackTraceExposureCodemod.class,
        SwitchLiteralFirstComparisonsCodemod.class,
        UnverifiedJwtCodemod.class,
        UpgradeSSLContextTLSCodemod.class,
        UpgradeSSLEngineTLSCodemod.class,
        UpgradeSSLParametersTLSCodemod.class,
        UpgradeSSLSocketProtocolsTLSCodemod.class,
        UpgradeTempFileToNIOCodemod.class,
        ValidateJakartaForwardPathCodemod.class,
        VerbTamperingCodemod.class);
  }

  /**
   * Entry point for core codemods.
   *
   * @param args the arguments to pass to the runner
   */
  public static void main(final String[] args) {
    Runner.run(asList(), args);
  }
}
