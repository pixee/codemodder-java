package io.pixee.codetl.java;

import io.pixee.codefixer.java.VisitorFactory;
import io.pixee.codefixer.java.protections.MyDSLVisitor;
import io.pixee.codetl.dsl.DSLLexer;
import io.pixee.codetl.dsl.DSLParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;

public class VisitorBasedDSLProcessor implements DSL {
  @Override
  public VisitorFactory parse(String input) {
    CharStream chars = CharStreams.fromString(input);

    var lexer = new DSLLexer(chars);
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    var parser = new DSLParser(tokens);
    ParseTree tree = parser.start();

    var calculator = new MyDSLVisitor();
    return calculator.visit(tree);
  }
}
