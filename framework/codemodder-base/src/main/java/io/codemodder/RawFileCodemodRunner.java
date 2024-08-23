package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/**
 * {@inheritDoc}
 *
 * <p>This type specializes in non-Java file.
 */
final class RawFileCodemodRunner implements CodemodRunner {

  private final RawFileChanger changer;
  private final IncludesExcludes rootedFileMatcher;

  RawFileCodemodRunner(final RawFileChanger changer, final Path projectDir) {
    this.changer = Objects.requireNonNull(changer);
    this.rootedFileMatcher = changer.getIncludesExcludesPattern().getRootedMatcher(projectDir);
  }

  RawFileCodemodRunner(
      final RawFileChanger changer, final IncludesExcludes globalIncludesExcludes) {
    this.changer = Objects.requireNonNull(changer);
    this.rootedFileMatcher = Objects.requireNonNull(globalIncludesExcludes);
  }

  @Override
  public boolean supports(final Path path) {
    return rootedFileMatcher.shouldInspect(path.toFile()) && changer.supports(path);
  }

  @Override
  public CodemodFileScanningResult run(final CodemodInvocationContext context) throws IOException {
    return changer.visitFile(context);
  }
}
