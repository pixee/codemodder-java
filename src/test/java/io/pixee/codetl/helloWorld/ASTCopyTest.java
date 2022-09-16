package io.pixee.codetl.helloWorld;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.ast.Value;
import io.pixee.codetl.base.HelloWordEnd2EndTrafoTest;
import io.pixee.lang.PrimitiveType;
import io.pixee.languages.helloworld.HelloWorldLanguage;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

final class ASTCopyTest extends HelloWordEnd2EndTrafoTest {

    @Test
    void it_copies_the_AST_correctly() {
        String helloWorldCode = """
                var x = 10
                var y = 20
                """;
        CodeUnit original = parseHelloWorldProgram(helloWorldCode);
        CodeUnit copy = original.copy();

        assertThat(original.root.dump(""), is(copy.root.dump("")));
    }

    @Test
    void it_makes_programs() {
        CodeUnit codeUnit = makeProgram();
        assertThat(codeUnit, is(not(nullValue())));
    }

    static CodeUnit makeProgram() {
        HelloWorldLanguage hw = HelloWorldLanguage.INSTANCE;
        CodeUnit code = new CodeUnit(hw.LANG, new Node(hw.PROGRAM));
        Node varX = new Node(hw.VAR)
                .add("initial", new Node(hw.NUM_LIT)
                        .add("value", new Value(PrimitiveType.STRING, "10")));
        varX.add("name", new Value(PrimitiveType.STRING, "x"));
        Node varY = new Node(hw.VAR)
                .add("initial", new Node(hw.NUM_LIT)
                        .add("value", new Value(PrimitiveType.STRING, "20")));
        varY.add("name", new Value(PrimitiveType.STRING, "y"));

        code.root.add("variables", varX);
        code.root.add("variables", varY);
        return code.resolve();
    }
}
