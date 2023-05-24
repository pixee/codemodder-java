package io.codemodder;

import io.codemodder.codetf.CodeTFReference;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** The base of a codemod type. */
public interface CodeChanger {

  /** The headline for this codemod's changes. */
  String getSummary();

  /** A deep description of what this codemod's changes. */
  String getDescription();

  /** The URL of the source code of the security control API added in this change, if any. */
  Optional<String> getSourceControlUrl();

  /**
   * A list of references for further reading on the issues this codemod addresses or other
   * supplementary information.
   */
  List<CodeTFReference> getReferences();

  /** A description of an individual change made by this codemod. */
  String getIndividualChangeDescription(final Path filePath, final CodemodChange change);
}
