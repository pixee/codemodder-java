package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class CodemodRegulatorTest {

  @Test
  void it_respects_order_on_includes() {
    CodemodRegulator regulator =
        CodemodRegulator.of(DefaultRuleSetting.DISABLED, List.of("c", "a", "b"));
    Optional<List<String>> desiredCodemodIdOrder = regulator.desiredCodemodIdOrder();
    assertThat(desiredCodemodIdOrder).isPresent();
    List<String> includeOrder = desiredCodemodIdOrder.get();
    assertThat(includeOrder).containsExactly("c", "a", "b");
  }

  @Test
  void it_doesnt_opine_on_order_when_excludes() {
    CodemodRegulator regulator =
        CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of("c", "a", "b"));
    Optional<List<String>> desiredCodemodIdOrder = regulator.desiredCodemodIdOrder();
    assertThat(desiredCodemodIdOrder).isEmpty();
  }
}
