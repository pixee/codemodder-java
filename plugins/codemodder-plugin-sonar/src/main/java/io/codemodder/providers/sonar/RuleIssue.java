package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.SonarFinding;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RuleIssue implements RuleFinding {

  private final Map<String, List<SonarFinding>> findings;

  RuleIssue(final Map<String, List<SonarFinding>> findings) {
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
