package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * {@inheritDoc}
 *
 * <p>This type specializes in non-Java file.
 */
final class RawFileCodemodRunner implements CodemodRunner {

  private final RawFileChanger changer;

  RawFileCodemodRunner(final RawFileChanger changer) {
    this.changer = Objects.requireNonNull(changer);
  }

  @Override
  public boolean supports(final Path path) {
    return true;
  }

  @Override
  public List<CodemodChange> run(final CodemodInvocationContext context) throws IOException {
    return changer.visitFile(context);
  }
}
