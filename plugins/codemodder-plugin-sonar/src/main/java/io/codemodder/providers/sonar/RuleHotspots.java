package io.codemodder.providers.sonar;

import triage.Hotspot;

import java.nio.file.Path;
import java.util.List;

/** A view of the Sonar hotspot results file for a given rule. */
public interface RuleHotspots {

  /** A list of issues associated with the given path. */
  List<Hotspot> getResultsByPath(Path path);

  /** Whether any results are available. */
  boolean hasResults();
}
