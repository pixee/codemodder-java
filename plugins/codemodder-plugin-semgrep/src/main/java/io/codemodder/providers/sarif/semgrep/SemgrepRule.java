package io.codemodder.providers.sarif.semgrep;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Represents a Semgrep rule with all the metadata it needs to be run and associated with a scan.
 */
record SemgrepRule(SemgrepScan semgrepScan, String ruleId, Path yaml) {

  SemgrepRule {
    Objects.requireNonNull(semgrepScan);
    Objects.requireNonNull(ruleId);
    Objects.requireNonNull(yaml);
  }
}
