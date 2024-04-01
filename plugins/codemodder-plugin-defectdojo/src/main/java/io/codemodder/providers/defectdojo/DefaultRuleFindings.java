package io.codemodder.providers.defectdojo;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class DefaultRuleFindings implements RuleFindings {

  private final Map<Path, List<Finding>> findingsByPath;

  DefaultRuleFindings(final List<Finding> findings) {
    // create a map of findings by their path to the list of findings for that path
    this.findingsByPath =
        findings.stream().collect(Collectors.groupingBy(f -> Path.of(f.getFilePath())));
  }

  @Override
  public List<Finding> getForPath(final Path path) {
    return findingsByPath.getOrDefault(path, List.of());
  }

  @Override
  public boolean isEmpty() {
    return findingsByPath.isEmpty();
  }
}
