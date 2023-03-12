package io.codemodder;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

/** {@inheritDoc} */
final class DefaultCodeDirectory implements CodeDirectory {

  private final Path repositoryDir;

  DefaultCodeDirectory(final Path repositoryDir) {
    this.repositoryDir = Objects.requireNonNull(repositoryDir);
  }

  @Override
  public File asFile() {
    return repositoryDir.toFile();
  }

  @Override
  public Path asPath() {
    return repositoryDir;
  }
}
