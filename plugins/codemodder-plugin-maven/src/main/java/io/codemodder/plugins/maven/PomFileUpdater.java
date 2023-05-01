package io.codemodder.plugins.maven;

import io.codemodder.DependencyGAV;
import io.codemodder.codetf.CodeTFChangesetEntry;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** A type responsible for making sure a pom has dependencies. */
interface PomFileUpdater {

  /**
   * Given a pom file, make sure it has the given dependencies, and update it if it doesn't.
   *
   * @param pomPath the path to the pom file
   * @param dependencies the dependencies that need to be injected (if necessary) into the pom
   * @throws IOException if there was an error reading or writing to the pom
   */
  Optional<CodeTFChangesetEntry> updatePom(
      Path projectDir, Path pomPath, List<DependencyGAV> dependencies) throws IOException;
}
