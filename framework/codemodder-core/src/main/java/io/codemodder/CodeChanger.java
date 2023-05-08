package io.codemodder;

import io.codemodder.codetf.CodeTFReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** The base of a codemod type. */
public interface CodeChanger {

  /** The headline for this codemod's changes. */
  default String getSummary() {
    return getClass().getName() + " - summary";
  }

  /** A deep description of what this codemod's changes. */
  default String getDescription() {
    return getClass().getName() + " - description";
  }

  /** The URL of the source code of the security control API added in this change, if any. */
  default Optional<String> getSourceControlUrl() {
    return Optional.empty();
  }

  /**
   * A list of references for further reading on the issues this codemod addresses or other
   * supplementary information.
   */
  default List<CodeTFReference> getReferences() {
    return List.of();
  }

  /** A description of an individual change made by this codemod. */
  default String getIndividualChangeDescription(final Path filePath, final CodemodChange change) {
    return "";
  }
}
