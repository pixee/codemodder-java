package io.pixee.languages.helloworld;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.ast.Value;
import io.pixee.codetl_antlr.helloWorldGrammarBaseVisitor;
import io.pixee.codetl_antlr.helloWorldGrammarParser;
import io.pixee.lang.PrimitiveType;

/**
 * Constructs the AST for Hello World programs from the parser result
 */
public class HelloWorldASTBuilderVisitor extends helloWorldGrammarBaseVisitor<Node> {

    private CodeUnit unit;
    private HelloWorldLanguage hw;

    public CodeUnit getCode() {
        return unit.resolve();
    }

    @Override public Node visitProgram(helloWorldGrammarParser.ProgramContext ctx) {
        hw = HelloWorldLanguage.INSTANCE;
        Node program = new Node(hw.PROGRAM);
        unit = new CodeUnit(hw.LANG, program);
        for (helloWorldGrammarParser.VariableContext c: ctx.variable()) {
            program.add("variables", createVariable(c));
        }
        return visitChildren(ctx);
    }

    private Node createVariable(helloWorldGrammarParser.VariableContext ctx) {
        Node var = new Node(hw.VAR);
        var.add("name", new Value(PrimitiveType.STRING, ctx.ID().getText()));
        var.add("initial", new Node(hw.NUM_LIT).add("value", new Value(PrimitiveType.STRING, ctx.numlit().NUMBER().getText())));
        return var;
    }

}
