package io.codemodder.providers.sonar;

import io.codemodder.providers.sonar.api.Issue;
import java.nio.file.Path;
import java.util.List;

/** A view of the issues for a given rule. */
public interface RuleIssues {

  /** A list of issues associated with the given path. */
  List<Issue> getResultsByPath(Path path);
}
