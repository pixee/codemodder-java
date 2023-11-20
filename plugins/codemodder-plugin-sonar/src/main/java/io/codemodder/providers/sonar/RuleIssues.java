package io.codemodder.providers.sonar;

import io.codemodder.providers.sonar.api.Issue;
import java.nio.file.Path;
import java.util.List;

public interface RuleIssues {

  List<Issue> getResultsByPath(Path path);
}
