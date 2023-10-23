package io.codemodder.providers.sarif.semgrep;

import java.nio.file.Path;
import java.util.Objects;

record SemgrepRule(SemgrepScan semgrepScan, String ruleId, Path yaml) {

  SemgrepRule {
    Objects.requireNonNull(semgrepScan);
    Objects.requireNonNull(ruleId);
    Objects.requireNonNull(yaml);
  }
}
