package io.pixee.codetl.helloWorld;

import io.pixee.ast.CodeUnit;
import io.pixee.codetl.CodeTLRuleDefinition;
import io.pixee.codetl.base.End2EndTrafoTest;
import io.pixee.codetl_antlr.helloWorldGrammarLexer;
import io.pixee.codetl_antlr.helloWorldGrammarParser;
import io.pixee.languages.helloworld.HelloWorldASTBuilderVisitor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

public class HelloWordEnd2EndTrafoTest extends End2EndTrafoTest {

    protected helloWorldGrammarParser getHelloWorldParser(final String input) {
        return new helloWorldGrammarParser(new CommonTokenStream(new helloWorldGrammarLexer(CharStreams.fromString(input))));
    }

    protected CodeUnit parseHelloWorldProgram(final String input) {
        HelloWorldASTBuilderVisitor visitor = new HelloWorldASTBuilderVisitor();
        visitor.visitProgram(getHelloWorldParser(input).program());
        return visitor.getCode();
    }

    protected CodeUnit transformCodeWithRule(String helloWorldCode, String ruleCode) {
        CodeUnit program = parseHelloWorldProgram(helloWorldCode);
        CodeTLRuleDefinition rule = parseCodeTL(ruleCode);
        transform(rule, program);
        return program;
    }

    protected String transformCodeWithRuleToString(String helloWorldCode, String ruleCode) {
        CodeUnit program = parseHelloWorldProgram(helloWorldCode);
        CodeTLRuleDefinition rule = parseCodeTL(ruleCode);
        transform(rule, program);
        return program.root.dump("");
    }

}
