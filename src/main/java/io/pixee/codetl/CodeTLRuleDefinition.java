package io.pixee.codetl;

import io.pixee.ast.Node;

import java.util.Objects;

/**
 * The root object of a CodeTL AST.
 */
public final class CodeTLRuleDefinition {

    private final RuleId ruleId;
    private final Node nodeToMatch;
    private final Node replacementNode;

    public CodeTLRuleDefinition(
            final RuleId ruleId,
            final Node nodeToMatch,
            final Node replacementNode) {
        this.ruleId = Objects.requireNonNull(ruleId);
        this.nodeToMatch = Objects.requireNonNull(nodeToMatch);
        this.replacementNode = Objects.requireNonNull(replacementNode);
    }

    public RuleId getRuleId() {
        return ruleId;
    }

    public Node getNodeToMatch() {
        return nodeToMatch;
    }

    public Node getReplacementNode() {
        return replacementNode;
    }

    public static CodeTLRuleDefinitionBuilder builder() {
        return new CodeTLRuleDefinitionBuilder();
    }

    /**
     * Builder for {@link CodeTLRuleDefinition}.
     */
    public static class CodeTLRuleDefinitionBuilder {

        private RuleId ruleId;
        private Node nodeToMatch;
        private Node replacementNode;

        private CodeTLRuleDefinitionBuilder() { }

        public void setRuleId(final RuleId ruleId) {
            this.ruleId = Objects.requireNonNull(ruleId);
        }

        public void setNodeToMatch(final Node nodeToMatch) {
            this.nodeToMatch = Objects.requireNonNull(nodeToMatch);
        }

        public void setReplacementNode(final Node replacementNode) {
            this.replacementNode = Objects.requireNonNull(replacementNode);
        }

        public static CodeTLRuleDefinitionBuilder builder() {
            return new CodeTLRuleDefinitionBuilder();
        }

        public CodeTLRuleDefinition build() {
            return new CodeTLRuleDefinition(ruleId, nodeToMatch, replacementNode);
        }
    }
}
