package io.codemodder.providers.sonar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import io.codemodder.sonar.model.SonarFinding;

final class DefaultRuleFindings implements RuleFinding {

  private final Map<String, List<SonarFinding>> findings;

  DefaultRuleFindings(final Map<String, List<SonarFinding>> findings) {
    this.findings = Objects.requireNonNull(findings);
  }

  @Override
  public List<SonarFinding> getResultsByPath(final Path path) {
    return findings.get(path.toString());
  }

  @Override
  public boolean hasResults() {
    return !findings.isEmpty();
  }
}
