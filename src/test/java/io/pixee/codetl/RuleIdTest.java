package io.pixee.codetl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test aspects of {@link RuleId}.
 */
final class RuleIdTest {

    @Test
    void it_parses_rules() {
        RuleId ruleId = RuleId.fromRawRuleId("pixee:java/stuff");
        assertThat(ruleId.getNamespace(), equalTo("pixee"));
        assertThat(ruleId.getSubjectLanguage(), equalTo("java"));
        assertThat(ruleId.getCategory(), equalTo("stuff"));
        assertThat(ruleId.toIdentifier(), equalTo("pixee:java/stuff"));

        ruleId = RuleId.fromRawRuleId("other:python/this-dash-thing");
        assertThat(ruleId.getNamespace(), equalTo("other"));
        assertThat(ruleId.getSubjectLanguage(), equalTo("python"));
        assertThat(ruleId.getCategory(), equalTo("this-dash-thing"));
        assertThat(ruleId.toIdentifier(), equalTo("other:python/this-dash-thing"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "foo",
            "foo:thing",
            "foo:thing/",
            "foo:thing/?",
    })
    void it_fails_on_bad_rules(final String badRuleId) {
        assertThrows(IllegalArgumentException.class, () -> RuleId.fromRawRuleId(badRuleId));
    }
}