package io.codemodder.codemods;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class SSRFURLTest {
    public URL getURL() throws MalformedURLException {
        var input = (new Scanner(System.in)).nextLine();
        return new URL(input);
    }
}
