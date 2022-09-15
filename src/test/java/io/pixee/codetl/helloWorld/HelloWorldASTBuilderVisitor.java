package io.pixee.codetl.helloWorld;

import io.pixee.ast.CodeUnit;
import io.pixee.ast.Node;
import io.pixee.ast.Value;
import io.pixee.codetl.helloWorld.grammar.helloWorldGrammarBaseVisitor;
import io.pixee.codetl.helloWorld.grammar.helloWorldGrammarParser;
import io.pixee.meta.PrimitiveType;

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
