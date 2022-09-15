package io.pixee.codetl.helloWorld;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.ast.Value;
import io.pixee.codetl.helloWorld.grammar.helloWorldGrammarLexer;
import io.pixee.codetl.helloWorld.grammar.helloWorldGrammarParser;
import io.pixee.engine.Engine;
import io.pixee.engine.ReplacementTransformation;
import io.pixee.meta.PrimitiveType;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class HelloWorldPlayground {

    public static void main(String[] args) {
        var input = "var x = 10\n"+
                    "var y = 20\n";

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
        System.err.println("----");
        System.err.println(program);
    }

}
