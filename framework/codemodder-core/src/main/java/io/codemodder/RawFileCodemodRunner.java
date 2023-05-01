package io.codemodder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * {@inheritDoc}
 *
 * <p>This type specializes in non-Java file.
 */
final class RawFileCodemodRunner implements CodemodRunner {

  private final RawFileChanger changer;
  private final EncodingDetector encodingDetector;

  RawFileCodemodRunner(final RawFileChanger changer, final EncodingDetector encodingDetector) {
    this.changer = Objects.requireNonNull(changer);
    this.encodingDetector = Objects.requireNonNull(encodingDetector);
  }

  @Override
  public List<CodemodChange> run(final CodemodInvocationContext context) throws IOException {
    return changer.visitFile(context);
  }
}
