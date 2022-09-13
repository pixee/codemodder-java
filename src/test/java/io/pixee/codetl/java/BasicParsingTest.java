package io.pixee.codetl.java;

import org.junit.jupiter.api.Test;


final class BasicParsingTest {

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
        var factory = parser.parse(input);
        System.out.println("factory: " + factory.rule);
    }
}
