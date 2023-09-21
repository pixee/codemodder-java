package io.codemodder.plugins.maven;

import com.github.difflib.patch.AbstractDelta;
import java.util.List;

/**
 * A type for finding the most meaningful line to represent the injection point of a dependency into
 * the pom.
 */
interface ArtifactInjectionPositionFinder {

  /**
   * Find the most meaningful line to represent the injection point of a dependency into the pom.
   */
  int find(List<AbstractDelta<String>> deltas, String artifactId);
}
