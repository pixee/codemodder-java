package io.codemodder.plugins.jpms;

import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/** A type that is responsible for updating module-info.java files. */
interface ModuleInfoUpdater {

  /**
   * Given the file edited during codemod execution, attempt to update the dependencies in the
   * module-info.java file
   *
   * @param projectDir the root of the project
   * @param moduleInfoJava the module-info.java file path
   * @param fileBeingChanged the file that was edited and caused the dependency updates
   * @param remainingFileDependencies the dependencies that were requested but not yet added
   * @return the result of the update operation
   * @throws IOException if there is a problem reading or writing files
   */
  DependencyUpdateResult update(
      Path projectDir,
      Path moduleInfoJava,
      Path fileBeingChanged,
      List<DependencyGAV> remainingFileDependencies)
      throws IOException;
}
