package io.pixee.codetl.helloWorld;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.ast.Value;
import io.pixee.codetl_antlr.helloWorldGrammarLexer;
import io.pixee.codetl_antlr.helloWorldGrammarParser;
import io.pixee.engine.Engine;
import io.pixee.engine.ReplacementTransformation;
import io.pixee.lang.PrimitiveType;
import io.pixee.languages.helloworld.HelloWorldASTBuilderVisitor;
import io.pixee.languages.helloworld.HelloWorldLanguage;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

final class HelloWorldTransformationTest {

    @Test
    void it_transforms_simple() {
        var input = """
                var x = 10
                var y = 20
                """;

        var parser = new helloWorldGrammarParser(new CommonTokenStream(new helloWorldGrammarLexer(CharStreams.fromString(input))));
        HelloWorldASTBuilderVisitor visitor = new HelloWorldASTBuilderVisitor();
        visitor.visitProgram(parser.program());
        CodeUnit program = visitor.getCode();
        System.err.println(program);

        Engine e = new Engine();
        HelloWorldLanguage hw = HelloWorldLanguage.INSTANCE;
        e.registerTransformation(new ReplacementTransformation(
                new Node(hw.NUM_LIT),
                new Node(hw.NUM_LIT).add("value", new Value(PrimitiveType.STRING,"0"))
        ));
        e.transform(program);
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
