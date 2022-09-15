package io.pixee.codetl.java;

import io.pixee.codefixer.java.VisitorFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


final class BasicParsingTest {

    @Disabled
    @Test
    void get_parser() {
        var input = "rule pixee:java/secure-random\n" +
                "match\n" +
                "\tConstructorCall {\n" +
                "\ttarget = Random\n" +
                "\t}\n" +
                "replace $c with\n" +
                "\tConstructorCall {\n" +
                "\ttarget = java.security.SecureRandom\n" +
                "\t}\n";

        var parser = new JavaDSLParser();
        VisitorFactory parse = parser.parse(input);
    }
}
