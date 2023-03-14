package io.codemodder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class DefaultCodeDirectory implements CodeDirectory {

  private final Path repositoryDir;

  DefaultCodeDirectory(final Path repositoryDir) {
    if (!Files.exists(repositoryDir)) {
      throw new IllegalArgumentException("code directory doesn't exist");
    }
    if (!Files.isDirectory(repositoryDir)) {
      throw new IllegalArgumentException("code directory isn't a directory");
    }
    if (!Files.isReadable(repositoryDir)) {
      throw new IllegalArgumentException("code directory isn't readable");
    }

    this.repositoryDir = Objects.requireNonNull(repositoryDir);
  }

  @Override
  public Path asPath() {
    return repositoryDir;
  }
}
