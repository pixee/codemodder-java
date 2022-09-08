package io.pixee.codetl.java;

import org.junit.jupiter.api.Test;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import io.pixee.dsl.java.DSLLexer;
import io.pixee.dsl.java.DSLParser;
import io.pixee.codetl.java.JavaDSLListener;

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

        var chars = CharStreams.fromString(input);
        var lexer = new DSLLexer(chars);
        var tokens = new CommonTokenStream(lexer);

        var parser = new DSLParser(tokens);
        ParseTree tree = parser.start();

        ParseTreeWalker walker = new ParseTreeWalker();
        JavaDSLListener listener = new JavaDSLListener();
        walker.walk(listener, tree);
    }
}