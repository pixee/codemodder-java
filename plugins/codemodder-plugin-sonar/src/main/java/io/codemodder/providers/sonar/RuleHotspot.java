package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.SonarFinding;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RuleHotspot implements RuleFinding {

  private final Map<String, List<SonarFinding>> findings;

  RuleHotspot(final Map<String, List<SonarFinding>> findings) {
    this.findings = Objects.requireNonNull(findings);
  }

  @Override
  public List<Hotspot> getResultsByPath(final Path path) {
    return findings.get(path.toString()).stream().map(Hotspot.class::cast).toList();
  }

  @Override
  public boolean hasResults() {
    return !findings.isEmpty();
  }
}
