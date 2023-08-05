package io.codemodder;

import com.google.inject.AbstractModule;
import java.nio.file.Path;
import java.util.Objects;

/** Binds the repository directory for codemods. */
final class CodeDirectoryModule extends AbstractModule {

  private final Path repositoryDir;

  CodeDirectoryModule(final Path repositoryDir) {
    this.repositoryDir = Objects.requireNonNull(repositoryDir);
  }

  @Override
  protected void configure() {
    bind(CodeDirectory.class).toInstance(new DefaultCodeDirectory(repositoryDir));
  }
}
