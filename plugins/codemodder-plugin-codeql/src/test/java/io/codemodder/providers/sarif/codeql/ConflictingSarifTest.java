package io.codemodder.providers.sarif.codeql;

import io.codemodder.*;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ConflictingSarifTest {

  /**
   * Test that conflicting SARIFs can be combined, and will fail gracefully, only honoring the first
   * set of results found.
   */
  @Test
  void it_combines_sarifs_with_overlapping_keys(@TempDir Path tempDir) {
    List<Path> sarifFiles =
        List.of(
            Path.of("src/test/resources/conflicting-sarifs/codeql-0.sarif"),
            Path.of("src/test/resources/conflicting-sarifs/codeql-1.sarif"),
            Path.of("src/test/resources/conflicting-sarifs/codeql-2.sarif"),
            Path.of("src/test/resources/conflicting-sarifs/codeql-3.sarif"),
            Path.of("src/test/resources/conflicting-sarifs/codeql-4.sarif"),
            Path.of("src/test/resources/conflicting-sarifs/codeql-5.sarif"));

    Map<String, List<RuleSarif>> pathSarifMap =
        SarifParser.create().parseIntoMap(sarifFiles, CodeDirectory.from(tempDir));

    new CodemodLoader(
        List.of(),
        CodemodRegulator.of(DefaultRuleSetting.ENABLED, List.of()),
        tempDir,
        List.of(),
        List.of(),
        List.of(),
        pathSarifMap,
        List.of(),
        List.of(),
        null,
        null);
  }
}
