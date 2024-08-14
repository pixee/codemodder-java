package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.SonarFinding;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** A view of the Sonar findings results file for a given rule. */
public interface RuleFinding<T extends SonarFinding> {

  /** A list of findings associated with the given path. */
  List<T> getResultsByPath(Path path);

  /** Whether any results are available. */
  boolean hasResults();

  /** Get all file paths that have any results. */
  Set<String> getPaths();
}
