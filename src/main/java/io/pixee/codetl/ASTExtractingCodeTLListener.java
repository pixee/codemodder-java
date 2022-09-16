package io.pixee.codetl;

import io.pixee.ast.Node;
import io.pixee.codetl_antlr.CodeTLBaseListener;
import io.pixee.codetl_antlr.CodeTLParser;
import io.pixee.languages.helloworld.HelloWorldLanguage;
import io.pixee.languages.java.JavaLanguageProvider;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Map;
import java.util.Objects;

/**
 * This type is responsible for building an AST for a CodeTL rule from a given ANTLR4 listening event.
 */
public final class ASTExtractingCodeTLListener extends CodeTLBaseListener {

    private final CodeTLRuleDefinition.CodeTLRuleDefinitionBuilder ruleBuilder;
    private final Map<String,SubjectLanguageProvider> subjectLanguageProviderMap;
    private SubjectLanguageProvider subjectLanguageProvider;
    private Node nodeToMatch;
    private Node replacementNode;
    private RuleId ruleId;

    public ASTExtractingCodeTLListener(final CodeTLRuleDefinition.CodeTLRuleDefinitionBuilder ruleBuilder) {
        this.ruleBuilder = Objects.requireNonNull(ruleBuilder);
        this.subjectLanguageProviderMap = Map.of("java", new JavaLanguageProvider(),
                                                 "helloworld", new GenericLanguageProvider(HelloWorldLanguage.INSTANCE.LANG));
    }

    @Override
    public void exitRule_statement(final CodeTLParser.Rule_statementContext ctx) {
        if(ruleId != null) {
            throw new IllegalStateException("already had rule ID defined");
        }
        ruleId = RuleId.fromRawRuleId(ctx.getChild(1).getText());
        ruleBuilder.setRuleId(ruleId);

        if(!subjectLanguageProviderMap.containsKey(ruleId.getSubjectLanguage())) {
            throw new IllegalArgumentException("unsupported rule language");
        }
        this.subjectLanguageProvider = subjectLanguageProviderMap.get(ruleId.getSubjectLanguage());
        super.exitRule_statement(ctx);
    }

    @Override
    public void enterMatch_statement(final CodeTLParser.Match_statementContext ctx) {
        super.enterMatch_statement(ctx);
    }

    @Override
    public void exitMatch_statement(final CodeTLParser.Match_statementContext ctx) {
        super.exitMatch_statement(ctx);
        ParseTree node = ctx.getChild(1);
        this.nodeToMatch = subjectLanguageProvider.parseMatchNode(node);
        ruleBuilder.setNodeToMatch(nodeToMatch);
    }

    @Override
    public void exitReplace_statement(final CodeTLParser.Replace_statementContext ctx) {
        super.exitReplace_statement(ctx);
        ParseTree node = ctx.getChild(2);
        this.replacementNode = subjectLanguageProvider.parseReplacementNode(node);
        ruleBuilder.setReplacementNode(replacementNode);
    }

    @Override
    public void exitCodeTlRule(final CodeTLParser.CodeTlRuleContext ctx) {
        super.exitCodeTlRule(ctx);
    }

}
