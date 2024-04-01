package io.codemodder.providers.defectdojo;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DefaultRuleFindingsTest {

  @Test
  void it_finds_by_path() {
    Finding file1Finding = finding_in_path("path/to/file1");
    Finding anotherFile1Finding = finding_in_path("path/to/file1");
    List<Finding> findings =
        List.of(file1Finding, anotherFile1Finding, finding_in_path("path/to/file2"));

    DefaultRuleFindings ruleFindings = new DefaultRuleFindings(findings);
    assertThat(ruleFindings.getForPath(Path.of("path/to/file1")))
        .isEqualTo(List.of(file1Finding, file1Finding));

    assertThat(ruleFindings.getForPath(Path.of("path/to/file2")))
        .isEqualTo(List.of(finding_in_path("path/to/file2")));

    assertThat(ruleFindings.getForPath(Path.of("path/to/file3"))).isEqualTo(List.of());
  }

  private Finding finding_in_path(final String path) {
    return new Finding() {
      @Override
      public String getFilePath() {
        return path;
      }
    };
  }
}
