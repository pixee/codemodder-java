package io.pixee.codetl.java;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


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

        assertThat(factory.ruleId().equals("pixee:java/secure-random"), is(true));
    }
}
