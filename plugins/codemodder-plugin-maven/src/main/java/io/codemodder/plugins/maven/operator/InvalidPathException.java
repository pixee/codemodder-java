package io.codemodder.plugins.maven.operator;

import java.io.IOException;
import java.nio.file.Path;

/** Exception class for handling invalid file paths. */
class InvalidPathException extends IOException {
  private final String relativePath;

  /**
   * Constructs an InvalidPathException with the specified details.
   *
   * @param parentPath The parent directory path.
   * @param relativePath The relative path that is considered invalid.
   * @param loop Indicates whether the path forms a loop.
   */
  public InvalidPathException(Path parentPath, String relativePath, boolean loop) {
    super(
        "Invalid Relative Path "
            + relativePath
            + " (from "
            + parentPath.toAbsolutePath()
            + ") (loops? "
            + loop
            + ")");
    this.relativePath = relativePath;
  }

  public String getRelativePath() {
    return relativePath;
  }
}
