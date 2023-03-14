package io.codemodder;

import java.nio.file.Path;
import java.util.Objects;

/** {@inheritDoc} */
final class DefaultCodemodInvocationContext implements CodemodInvocationContext {

  private final CodeDirectory codeDirectory;
  private final Path path;
  private final String codemodId;
  private final FileWeavingContext changeRecorder;

  DefaultCodemodInvocationContext(
      final CodeDirectory codeDirectory,
      final Path path,
      final String codemodId,
      final FileWeavingContext changeRecorder) {
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
    this.path = Objects.requireNonNull(path);
    this.codemodId = Objects.requireNonNull(codemodId);
    this.changeRecorder = Objects.requireNonNull(changeRecorder);
  }

  /** {@inheritDoc} */
  @Override
  public FileWeavingContext changeRecorder() {
    return changeRecorder;
  }

  /** {@inheritDoc} */
  @Override
  public CodeDirectory codeDirectory() {
    return codeDirectory;
  }

  /** {@inheritDoc} */
  @Override
  public Path path() {
    return path;
  }

  /** {@inheritDoc} */
  @Override
  public String codemodId() {
    return codemodId;
  }
}
