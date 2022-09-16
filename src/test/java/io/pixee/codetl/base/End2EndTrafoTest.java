package io.pixee.codetl.base;

import io.pixee.ast.CodeUnit;
import io.pixee.codetl.ASTExtractingCodeTLListener;
import io.pixee.codetl.CodeTLRuleDefinition;
import io.pixee.codetl_antlr.CodeTLBaseListener;
import io.pixee.codetl_antlr.CodeTLLexer;
import io.pixee.codetl_antlr.CodeTLParser;
import io.pixee.engine.Engine;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

abstract public class End2EndTrafoTest {

    protected CodeTLParser getCodeTLParser(final String input) {
        CodePointCharStream stream = CharStreams.fromString(input);
        CodeTLLexer lexer = new CodeTLLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new CodeTLParser(tokens);
    }

    protected CodeTLRuleDefinition parseCodeTL(final String codeTLCode) {
        CodeTLParser.CodeTlRuleContext parsedRule = getCodeTLParser(codeTLCode).codeTlRule();
        final CodeTLRuleDefinition.CodeTLRuleDefinitionBuilder ruleBuilder = CodeTLRuleDefinition.builder();
        CodeTLBaseListener listener = new ASTExtractingCodeTLListener(ruleBuilder);
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, parsedRule);
        return ruleBuilder.build();
    }

    protected void transform(CodeTLRuleDefinition rule, CodeUnit code) {
        Engine e = new Engine();
        e.registerRule(rule);
        e.transformWithRules(code);
        code.resolve();
    }


}
