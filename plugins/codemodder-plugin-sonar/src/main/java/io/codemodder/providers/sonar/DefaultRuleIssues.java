package io.codemodder.providers.sonar;

import io.codemodder.providers.sonar.api.Issue;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class DefaultRuleIssues implements RuleIssues {

  private final Map<String, List<Issue>> issues;

  DefaultRuleIssues(final Map<String, List<Issue>> issues) {
    this.issues = Objects.requireNonNull(issues);
  }

  @Override
  public List<Issue> getResultsByPath(final Path path) {
    return issues.get(path.toString());
  }

  @Override
  public boolean hasResults() {
    return !issues.isEmpty();
  }
}
