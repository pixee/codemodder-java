package io.pixee.codefixer.java;

import java.util.List;
import java.util.Objects;

/**
 * A type that is relied on to inform our in-flight analysis.
 */
public interface RuleContext {

    /**
     * Taking into account the configuration, understand if this rule is currently allowed.
     * @param ruleId the string id of the rule
     * @return true, if the rule is allowed -- false otherwise
     */
    boolean isRuleAllowed(String ruleId);

    class DefaultRuleContext implements RuleContext {

        private final DefaultRuleSetting setting;
        private final List<String> exceptions;

        DefaultRuleContext(final DefaultRuleSetting defaultRuleSetting, final List<String> ruleExceptions) {
            this.setting = Objects.requireNonNull(defaultRuleSetting);
            this.exceptions = Objects.requireNonNull(ruleExceptions);
        }

        @Override
        public boolean isRuleAllowed(final String ruleId) {
            if(DefaultRuleSetting.ENABLED.equals(setting)) {
                return !exceptions.contains(ruleId);
            }
            return exceptions.contains(ruleId);
        }
    }

    static RuleContext of(final DefaultRuleSetting defaultRuleSetting, final List<String> ruleExceptions) {
        return new DefaultRuleContext(defaultRuleSetting, ruleExceptions);
    }

}
