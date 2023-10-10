package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.io.IOException;

public class InvalidPathException extends IOException {
  private final File parentPath;
  private final String relativePath;
  private final boolean loop;

  public InvalidPathException(File parentPath, String relativePath, boolean loop) {
    super(
        "Invalid Relative Path "
            + relativePath
            + " (from "
            + parentPath.getAbsolutePath()
            + ") (loops? "
            + loop
            + ")");
    this.parentPath = parentPath;
    this.relativePath = relativePath;
    this.loop = loop;
  }

  public File getParentPath() {
    return parentPath;
  }

  public String getRelativePath() {
    return relativePath;
  }

  public boolean isLoop() {
    return loop;
  }
}
