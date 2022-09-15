package io.pixee.codetl.helloWorld;

import io.pixee.codetl.helloWorld.grammar.helloWorldGrammarLexer;
import io.pixee.codetl.helloWorld.grammar.helloWorldGrammarParser;
import io.pixee.codetl.java.JavaDSLListener;
import io.pixee.dsl.java.DSLLexer;
import io.pixee.dsl.java.DSLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.Test;

final class BasicParsingTest {

    @Test
    void get_parser() {
        var input = "var x = 10\n"+
                    "var y = 20\n";

        var chars = CharStreams.fromString(input);
        var lexer = new helloWorldGrammarLexer(chars);
        var tokens = new CommonTokenStream(lexer);

        var parser = new helloWorldGrammarParser(tokens);
        HelloWorldASTBuilderVisitor visitor = new HelloWorldASTBuilderVisitor();
        visitor.visitProgram(parser.program());
        System.err.println(visitor.getCode().toString());
    }
}