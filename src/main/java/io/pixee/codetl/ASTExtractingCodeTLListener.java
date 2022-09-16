package io.pixee.codetl;

import io.pixee.ast.Node;
import io.pixee.codetl_antlr.CodeTLBaseListener;
import io.pixee.codetl_antlr.CodeTLParser;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.Map;
import java.util.Objects;

/**
 * This type is responsible for building an AST for a CodeTL rule from a given ANTLR4 listening event.
 */
final class ASTExtractingCodeTLListener extends CodeTLBaseListener {

    private final CodeTLRuleDefinition.CodeTLRuleDefinitionBuilder ruleBuilder;
    private final Map<String,SubjectLanguageProvider> subjectLanguageProviderMap;
    private SubjectLanguageProvider subjectLanguageProvider;
    private Node nodeToMatch;
    private Node replacementNode;
    private String subjectLanguage;
    private RuleId ruleId;

    public ASTExtractingCodeTLListener(final CodeTLRuleDefinition.CodeTLRuleDefinitionBuilder ruleBuilder) {
        this.ruleBuilder = Objects.requireNonNull(ruleBuilder);
        this.subjectLanguageProviderMap = Map.of("java", new JavaLanguageProvider());
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
    }

    @Override
    public void exitReplace_statement(final CodeTLParser.Replace_statementContext ctx) {
        super.exitReplace_statement(ctx);
        ParseTree node = ctx.getChild(1);
        this.replacementNode = subjectLanguageProvider.parseReplacementNode(node);
    }

    @Override
    public void exitCodeTlRule(final CodeTLParser.CodeTlRuleContext ctx) {
        super.exitCodeTlRule(ctx);
    }

    /**
     * This type is responsible for parsing the ANTLR model objects into CodeTL node objects.
     */
    interface SubjectLanguageProvider {
        Node parseMatchNode(ParseTree node);
        Node parseReplacementNode(ParseTree node);
    }

    /**
     * {@inheritDoc}
     *
     * Responsible for the Java language!
     */
    static class JavaLanguageProvider implements SubjectLanguageProvider {

        @Override
        public Node parseMatchNode(final ParseTree node) {
            String languageNodeType = node.getChild(0).getText();
            if("StaticMethodCall".equals(languageNodeType)) {
                return parseStaticMethodCall(node);
            }
            throw new UnsupportedOperationException();
        }

        /**
         * Turn a <code>StaticMethodCall</code> into a parsed node.
         */
        private Node parseStaticMethodCall(final ParseTree methodCall) {
            Node node = null;
            return node;
        }

        @Override
        public Node parseReplacementNode(final ParseTree node) {
            throw new UnsupportedOperationException();
        }
    }

}
