package io.pixee.codetl;

import io.pixee.ast.ErrorNode;
import io.pixee.ast.Node;
import io.pixee.codetl_antlr.CodeTLBaseListener;
import io.pixee.codetl_antlr.CodeTLLexer;
import io.pixee.codetl_antlr.CodeTLParser;
import io.pixee.languages.helloworld.HelloWorldLanguage;
import io.pixee.tools.ASTStructureChecker;
import io.pixee.tools.Message;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

final class CodeTLParserTestForHelloWorld {


    @Test
    void it_parses_generic_node() {
        var input = """
                rule pixee:helloworld/stuff
                match
                    NumLit $n {}
                replace $n 
                    NumLit {
                        value = 10
                    }    
                """;
        CodeTLRuleDefinition rule = parseRule(input);
        Node nodeToMatch = rule.getNodeToMatch();
        assertThat(nodeToMatch.concept, equalTo(HelloWorldLanguage.INSTANCE.NUM_LIT));
    }

    @Test
    void it_produces_error_node() {
        var input = """
                rule pixee:helloworld/stuff
                match
                    Junk $n {}
                replace $n
                    NumLit {
                        value = 10
                    }
                """;
        CodeTLRuleDefinition rule = parseRule(input);
        Node nodeToMatch = rule.getNodeToMatch();
        assertThat(nodeToMatch, instanceOf(ErrorNode.class));
        Iterable<Message> errors = new ASTStructureChecker(HelloWorldLanguage.INSTANCE.LANG).run(nodeToMatch);
        assertThat(errors.iterator().hasNext(), is(true));
        assertThat(errors.iterator().next().toString(), is("concept named Junk not found in language HelloWorld"));
    }

    @NotNull
    private CodeTLRuleDefinition parseRule(final String input) {
        CodePointCharStream stream = CharStreams.fromString(input);
        CodeTLLexer lexer = new CodeTLLexer(stream);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CodeTLParser parser = new CodeTLParser(tokens);
        CodeTLParser.CodeTlRuleContext parsedRule = parser.codeTlRule();

        final CodeTLRuleDefinition.CodeTLRuleDefinitionBuilder ruleBuilder = CodeTLRuleDefinition.builder();
        CodeTLBaseListener listener = new ASTExtractingCodeTLListener(ruleBuilder);

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(listener, parsedRule);
        return ruleBuilder.build();
    }



}
