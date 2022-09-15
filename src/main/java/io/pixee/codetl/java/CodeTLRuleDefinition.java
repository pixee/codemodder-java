package io.pixee.codetl.java;

import java.util.Objects;

/**
 * The root object of a rule AST.
 */
public final class CodeTLRuleDefinition {

    private final String ruleId;

    public CodeTLRuleDefinition(final String ruleId) {
        this.ruleId = Objects.requireNonNull(ruleId);
    }

    public String getRuleId() {
        return ruleId;
    }
}
