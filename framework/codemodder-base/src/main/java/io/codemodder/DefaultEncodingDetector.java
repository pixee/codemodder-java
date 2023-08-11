package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.mozilla.universalchardet.UniversalDetector;

final class DefaultEncodingDetector implements EncodingDetector {
  @Override
  public Optional<String> detect(final Path originalJavaFile) throws IOException {
    String encoding = UniversalDetector.detectCharset(originalJavaFile);
    return Optional.ofNullable(encoding);
  }
}
