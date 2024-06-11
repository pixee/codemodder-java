package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.Issue;
import java.nio.file.Path;
import java.util.List;
import io.codemodder.sonar.model.Hotspot;

/** A view of the Sonar hotspot results file for a given rule. */
public interface RuleHotspots {

  /** A list of issues associated with the given path. */
  List<Hotspot> getResultsByPath(Path path);

  /** Whether any results are available. */
  boolean hasResults();
}
