package io.codemodder.providers.defectdojo;

import java.nio.file.Path;
import java.util.List;

/** Represents the findings of a given rule. */
public interface RuleFindings {

  /** Returns the findings for the given path. */
  List<Finding> getForPath(Path path);

  /** Returns true if there are no findings. */
  boolean isEmpty();
}
