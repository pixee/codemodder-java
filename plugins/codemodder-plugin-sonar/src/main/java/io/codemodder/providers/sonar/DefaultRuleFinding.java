package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.SonarFinding;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

abstract class DefaultRuleFinding<T extends SonarFinding> implements RuleFinding<T> {

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

  @Override
  public List<String> getPaths() {
    var pathSet = new HashSet<Path>();
    Stream<T> allFindings = findings.values().stream().flatMap(l -> l.stream());
    return allFindings.flatMap(f -> f.componentFileName().stream()).distinct().toList();
  }
}
