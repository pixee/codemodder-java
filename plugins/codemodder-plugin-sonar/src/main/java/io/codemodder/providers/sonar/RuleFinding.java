package io.codemodder.providers.sonar;

import java.nio.file.Path;
import java.util.List;

import io.codemodder.sonar.model.SonarFinding;

/** A view of the Sonar findings results file for a given rule. */
public interface RuleFinding {

  /** A list of issues associated with the given path. */
  List<? extends SonarFinding> getResultsByPath(Path path);

  /** Whether any results are available. */
  boolean hasResults();
}
