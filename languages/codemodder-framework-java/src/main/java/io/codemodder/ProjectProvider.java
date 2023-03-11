package io.codemodder;

import java.util.Optional;

/** A provider that offers project information, e.g., the compiler's Java source code level. */
public interface ProjectProvider {

  /**
   * @return the Java source version the project build file appears to target.
   */
  Optional<String> javaSourceTargetVersion();

  /**
   * @return the Java bytecode version the project build file appears to target.
   */
  Optional<String> javaBytecodeTargetVersion();
}
