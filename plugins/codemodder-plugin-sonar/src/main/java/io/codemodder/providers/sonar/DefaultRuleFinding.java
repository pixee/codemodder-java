package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.SonarFinding;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

class DefaultRuleFinding<T extends SonarFinding> implements RuleFinding<T> {

  private final Map<String, List<T>> findings;

  DefaultRuleFinding(List<T> sonarFindings, final Path repository) {
    Map<String, List<T>> findingsByPath = new HashMap<>();
    sonarFindings.forEach(
        finding -> {
          Optional<String> filename = finding.componentFileName();
          if (filename.isPresent()) {
            String fullPath = repository.resolve(filename.get()).toString();
            List<T> pathFindings = findingsByPath.computeIfAbsent(fullPath, f -> new ArrayList<>());
            pathFindings.add(finding);
          }
        });

    this.findings = Objects.requireNonNull(findingsByPath);
  }

  @Override
  public List<T> getResultsByPath(final Path path) {
    return findings.get(path.toString());
  }

  @Override
  public boolean hasResults() {
    return !findings.isEmpty();
  }
}
