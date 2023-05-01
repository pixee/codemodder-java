package io.codemodder;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Detects the encoding of a file. */
public interface EncodingDetector {
  /** Try to detect the encoding of a file. */
  Optional<String> detect(Path file) throws IOException;

  static EncodingDetector create() {
    return new DefaultEncodingDetector();
  }
}
