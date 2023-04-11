package io.openpixee.java;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import io.codemodder.CodemodRegulator;
import io.codemodder.DefaultRuleSetting;
import java.util.List;
import org.junit.jupiter.api.Test;

final class CodemodRegulatorTest {

  @Test
  void it_honors_enabled_with_no_exceptions() {
    CodemodRegulator context = CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of());
    assertThat(context.isAllowed("rule1"), equalTo(true));
    assertThat(context.isAllowed("rule2"), equalTo(true));
  }

  @Test
  void it_honors_enabled_with_exceptions() {
    CodemodRegulator context = CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of("rule1"));
    assertThat(context.isAllowed("rule1"), equalTo(false));
    assertThat(context.isAllowed("rule2"), equalTo(true));
  }

  @Test
  void it_honors_disabled_with_no_exceptions() {
    CodemodRegulator context = CodemodRegulator.of(DefaultRuleSetting.DISABLED, List.of());
    assertThat(context.isAllowed("rule1"), equalTo(false));
    assertThat(context.isAllowed("rule2"), equalTo(false));
  }

  @Test
  void it_honors_disabled_with_exceptions() {
    CodemodRegulator context = CodemodRegulator.of(DefaultRuleSetting.DISABLED, List.of("rule1"));
    assertThat(context.isAllowed("rule1"), equalTo(true));
    assertThat(context.isAllowed("rule2"), equalTo(false));
  }
}
