package io.openpixee.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.openpixee.codetl.config.DefaultRuleSetting;
import java.util.List;
import org.junit.jupiter.api.Test;

final class RuleContextTest {

  @Test
  void it_honors_enabled_with_no_exceptions() {
    RuleContext context = RuleContext.of(DefaultRuleSetting.ENABLED, List.of());
    assertThat(context.isRuleAllowed("rule1"), equalTo(true));
    assertThat(context.isRuleAllowed("rule2"), equalTo(true));
  }

  @Test
  void it_honors_enabled_with_exceptions() {
    RuleContext context = RuleContext.of(DefaultRuleSetting.ENABLED, List.of("rule1"));
    assertThat(context.isRuleAllowed("rule1"), equalTo(false));
    assertThat(context.isRuleAllowed("rule2"), equalTo(true));
  }

  @Test
  void it_honors_disabled_with_no_exceptions() {
    RuleContext context = RuleContext.of(DefaultRuleSetting.DISABLED, List.of());
    assertThat(context.isRuleAllowed("rule1"), equalTo(false));
    assertThat(context.isRuleAllowed("rule2"), equalTo(false));
  }

  @Test
  void it_honors_disabled_with_exceptions() {
    RuleContext context = RuleContext.of(DefaultRuleSetting.DISABLED, List.of("rule1"));
    assertThat(context.isRuleAllowed("rule1"), equalTo(true));
    assertThat(context.isRuleAllowed("rule2"), equalTo(false));
  }
}
