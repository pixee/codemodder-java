package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.sarif.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link SarifFindingKeyUtil}. */
final class SarifFindingKeyUtilTest {

  private Result result;

  @BeforeEach
  void before() {
    result = new Result();
    result.setRuleId("my-rule");
  }

  @Test
  void it_supports_findings_without_ids() {
    final var id = SarifFindingKeyUtil.buildFindingId(result);
    assertThat(id).isNull();
  }

  @Test
  void it_prefers_guid() {
    result.setGuid("my-guid");
    result.setCorrelationGuid("my-correlation-guid");
    final var id = SarifFindingKeyUtil.buildFindingId(result);
    assertThat(id).isEqualTo("my-guid");
  }

  @Test
  void it_falls_back_to_correlation_guid() {
    result.setCorrelationGuid("my-correlation-guid");
    final var id = SarifFindingKeyUtil.buildFindingId(result);
    assertThat(id).isEqualTo("my-correlation-guid");
  }
}
