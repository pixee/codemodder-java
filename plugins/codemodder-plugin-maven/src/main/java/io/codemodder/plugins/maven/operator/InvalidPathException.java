package io.codemodder.plugins.maven.operator;

import java.io.IOException;
import java.nio.file.Path;

/** Exception class for handling invalid file paths. */
class InvalidPathException extends IOException {
  private final Path parentPath;
  private final String relativePath;
  private final boolean loop;

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
    this.parentPath = parentPath;
    this.relativePath = relativePath;
    this.loop = loop;
  }

  public String getRelativePath() {
    return relativePath;
  }
}
