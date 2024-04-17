package io.codemodder;

import java.nio.file.Path;

/** Utility to generate Sarif finding's ID */
public class SarifFindingKeyUtil {

  private SarifFindingKeyUtil() {}

  /** Generates key given rule, path and line */
  public static String buildKey(final String rule, final Path path, final int line) {
    return String.format("%s-%s-%d", rule, path.getFileName(), line);
  }
}
