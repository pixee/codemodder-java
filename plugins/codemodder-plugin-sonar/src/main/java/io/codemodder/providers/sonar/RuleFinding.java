package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.SonarFinding;
import java.nio.file.Path;
import java.util.List;

/** A view of the Sonar findings results file for a given rule. */
interface RuleFinding {

  /** A list of findings associated with the given path. */
  List<? extends SonarFinding> getResultsByPath(Path path);

  /** Whether any results are available. */
  boolean hasResults();
}
