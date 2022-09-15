package io.pixee.codetl.java;

import io.pixee.codefixer.java.VisitorFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


final class BasicParsingTest {

    @Disabled
    @Test
    void get_parser() {
        var input = "rule pixee:java/secure-random\n" +
                "match\n" +
                "\tConstructorCall {\n" +
                "\ttarget = Random\n" +
                "\t}\n" +
                "replaceChild $c with\n" +
                "\tConstructorCall {\n" +
                "\ttarget = java.security.SecureRandom\n" +
                "\t}\n";

        var parser = new JavaDSLParser();
        VisitorFactory parse = parser.parse(input);
        var factory = parser.parse(input);

        assertThat(factory.ruleId().equals("pixee:java/secure-random"), is(true));
    }
}
