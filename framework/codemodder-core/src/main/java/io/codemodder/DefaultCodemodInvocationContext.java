package io.codemodder;

import java.nio.file.Path;
import java.util.Objects;

final class DefaultCodemodInvocationContext implements CodemodInvocationContext {

  private final CodeDirectory codeDirectory;
  private final Path path;
  private final String codemodId;
  private final LineIncludesExcludes lineIncludesExcludes;

  DefaultCodemodInvocationContext(
      final CodeDirectory codeDirectory,
      final Path path,
      final String codemodId,
      final LineIncludesExcludes lineIncludesExcludes) {
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
    this.path = Objects.requireNonNull(path);
    this.codemodId = Objects.requireNonNull(codemodId);
    this.lineIncludesExcludes = Objects.requireNonNull(lineIncludesExcludes);
  }

  @Override
  public LineIncludesExcludes lineIncludesExcludes() {
    return lineIncludesExcludes;
  }

  @Override
  public CodeDirectory codeDirectory() {
    return codeDirectory;
  }

  @Override
  public Path path() {
    return path;
  }

  @Override
  public String codemodId() {
    return codemodId;
  }
}
