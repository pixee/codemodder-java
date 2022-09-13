package io.pixee.codetl.java;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import io.pixee.dsl.java.DSLLexer;
import io.pixee.dsl.java.DSLParser;
import io.pixee.codetl.DSL;

import io.pixee.codefixer.java.VisitorFactory;


public class JavaDSLParser implements DSL {
    @Override
    public VisitorFactory parse(String input) {
        var chars = CharStreams.fromString(input);
        var lexer = new DSLLexer(chars);
        var tokens = new CommonTokenStream(lexer);

        var parser = new DSLParser(tokens);
        ParseTree tree = parser.start();

        ParseTreeWalker walker = new ParseTreeWalker();
        JavaDSLListener listener = new JavaDSLListener();
        walker.walk(listener, tree);

        return listener.result;
    }
}
