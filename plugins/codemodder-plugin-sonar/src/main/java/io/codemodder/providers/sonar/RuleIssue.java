package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.Issue;
import java.nio.file.Path;
import java.util.List;

public class RuleIssue extends DefaultRuleFinding<Issue> {
  RuleIssue(List<Issue> sonarFindings, final Path repository) {
    super(sonarFindings, repository);
  }
}
