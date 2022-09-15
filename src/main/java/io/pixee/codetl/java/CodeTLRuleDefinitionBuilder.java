package io.pixee.codetl.java;

import java.util.Objects;

public class CodeTLRuleDefinitionBuilder {

    private String ruleId;

    private CodeTLRuleDefinitionBuilder() { }

    public CodeTLRuleDefinitionBuilder withRuleId(final String ruleId) {
        this.ruleId = Objects.requireNonNull(ruleId);
        return this;
    }

    public static CodeTLRuleDefinitionBuilder builder() {
        return new CodeTLRuleDefinitionBuilder();
    }

    public CodeTLRuleDefinition build() {
        return new CodeTLRuleDefinition(ruleId);
    }
}
