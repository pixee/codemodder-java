package io.codemodder.remediation.zipslip;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

final class ZipEntryStartFixStrategyTest {

  /** This is the fix method we inject. */
  String sanitizeZipFilename(String entryName) {
    if (entryName == null || entryName.trim().isEmpty()) {
      return entryName;
    }
    while (entryName.contains("../") || entryName.contains("..\\")) {
      entryName = entryName.replace("../", "").replace("..\\", "");
    }
    return entryName;
  }

  @ParameterizedTest
  @CsvSource({
    // give some real file names we shouldn't mess with
    "test.zip, test.zip",
    "test-123.doc, test-123.doc",
    "path/to/test/file.txt, path/to/test/file.txt",

    // give some names with a path traversal
    "test/../../evil.txt, test/evil.txt",
    "test/../.../...//..//evil.txt, test//evil.txt",
    "test\\..\\..\\evil.txt, test\\evil.txt"
  })
  void it_should_sanitize_zip_filename(final String before, final String after) {
    assertThat(sanitizeZipFilename(before)).isEqualTo(after);
  }
}
