package io.pixee.codetl;

import io.pixee.codetl_antlr.CodeTLBaseListener;
import io.pixee.codetl_antlr.CodeTLLexer;
import io.pixee.codetl_antlr.CodeTLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

final class CodeTLParserTest {

    @ParameterizedTest
    @CsvSource({
            "rule pixee:java/stuff,pixee:java/stuff",
            "rule  pixee:java/stuff,pixee:java/stuff",
            "\nrule\tpixee:java/stuff,pixee:java/stuff",
            "\nrule\tpixee:java/dashed-thing,pixee:java/dashed-thing"
    })
    void it_compiles_correct(final String codetl, final String expectedRule) {
        CodeTLParser parser = getParser(codetl + " match StaticMethodCall $c {} replace $c StaticMethodCall {}");
        CodeTLParser.CodeTlRuleContext parsedRule = parser.codeTlRule();

        final CodeTLRuleDefinition.CodeTLRuleDefinitionBuilder ruleBuilder = CodeTLRuleDefinition.builder();
        CodeTLBaseListener listener = new ASTExtractingCodeTLListener(ruleBuilder);

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, parsedRule);

        CodeTLRuleDefinition rule = ruleBuilder.build();
        assertThat(rule.getRuleId().toIdentifier(), equalTo(expectedRule));
    }

    @Test
    void it_handles_import() {
        String codeTl = """
                rule pixee:java/stuff
                match
                   StaticMethodCall $c { }
                require import org.apache.Foo
                require import org.apache.Bar
                replace $c
                   StaticMethodCall { }
                """;
        CodeTLParser parser = getParser(codeTl);
        CodeTLParser.CodeTlRuleContext parsedRule = parser.codeTlRule();
        assertThat(parsedRule, is(notNullValue()));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "rule=pixee:java/stuff", // wrong separator
            "rule pixeejava/stuff", // no namespace separator
            "rule pixee:java_stuff", // no language separator
    })
    void it_doesnt_parse_invalid(final String badRuleId) {
        CodeTLParser parser = getParser(badRuleId);
    }

    @NotNull
    private CodeTLParser getParser(final String input) {
        CodePointCharStream stream = CharStreams.fromString(input);
        CodeTLLexer lexer = new CodeTLLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        return new CodeTLParser(tokens);
    }

}
