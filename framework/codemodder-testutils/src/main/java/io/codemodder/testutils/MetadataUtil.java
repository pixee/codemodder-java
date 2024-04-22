package io.codemodder.testutils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class MetadataUtil {

  private MetadataUtil() {}

  static void checkExpectedFixLinesUsage(
      final String testResourceDir,
      final int[] expectingFixesAtLines,
      final int[] expectingFailedFixesAtLines)
      throws IOException {
    List<Path> paths =
        Files.walk(Path.of("src/test/resources/", testResourceDir))
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".before"))
            .toList();

    if ((expectingFixesAtLines.length > 0 || expectingFailedFixesAtLines.length > 0)
        && paths.size() > 1) {
      throw new IllegalArgumentException(
          "Expected fixes at lines is not supported with multi-file test feature. Define single test when setting expected fix lines.");
    }
  }
}
