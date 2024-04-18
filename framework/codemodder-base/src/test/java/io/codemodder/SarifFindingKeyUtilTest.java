package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.sarif.Fingerprints;
import com.contrastsecurity.sarif.Result;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SarifFindingKeyUtilTest {

  @Test
  void it_creates_key(@TempDir final Path tmpDir) throws IOException {
    Path tmpFile = tmpDir.resolve("my-code.txt");
    Files.writeString(tmpFile, "my code");

    Result result = new Result();
    result.setRuleId("my-rule");

    assertThat(SarifFindingKeyUtil.buildFindingId(result, tmpFile, 1))
        .isEqualTo("my-rule-my-code.txt-1");

    Fingerprints fingerprints = new Fingerprints();
    fingerprints.setAdditionalProperty("my-key", "my-fingerprint-value");
    result.setFingerprints(fingerprints);
    assertThat(SarifFindingKeyUtil.buildFindingId(result, tmpFile, 1))
        .isEqualTo("my-fingerprint-value");

    result.setCorrelationGuid(" my-correlation-guid ");
    assertThat(SarifFindingKeyUtil.buildFindingId(result, tmpFile, 1))
        .isEqualTo("my-correlation-guid");

    result.setGuid("my-guid");
    assertThat(SarifFindingKeyUtil.buildFindingId(result, tmpFile, 1)).isEqualTo("my-guid");
  }
}
