package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.Issue;
import java.nio.file.Path;
import java.util.List;

/** Class type to bind {@link Issue} from {@link ProvidedSonarScan} */
public final class RuleIssue extends DefaultRuleFinding<Issue> {
  RuleIssue(List<Issue> issues, final Path repository) {
    super(issues, repository);
  }
}
