package io.codemodder.codemods;

import io.github.pixee.security.HostValidator;
import io.github.pixee.security.Urls;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class SSRFURLTest {
    public URL getURL() throws MalformedURLException {
        var input = (new Scanner(System.in)).nextLine();
        return Urls.create(input, Urls.HTTP_PROTOCOLS, HostValidator.DENY_COMMON_INFRASTRUCTURE_TARGETS);
    }
}
