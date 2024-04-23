package io.codemodder.testutils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

final class ExpectedFixesTest {

  private static final Path MULTIPLE_BEFORE_FILES_DIR =
      Path.of("src/test/resources/multiple-before-files");
  private static final Path SINGLE_BEFORE_FILE_DIR =
      Path.of("src/test/resources/single-before-file");

  @Test
  void it_expects_exception_fixes_multiple_before_files() {

    assertThatThrownBy(
            () ->
                ExpectedFixes.checkExpectedFixLinesUsage(
                    MULTIPLE_BEFORE_FILES_DIR, new int[] {1}, new int[] {}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void it_expects_exception_failed_fixes_multiple_before_files() {

    assertThatThrownBy(
            () ->
                ExpectedFixes.checkExpectedFixLinesUsage(
                    MULTIPLE_BEFORE_FILES_DIR, new int[] {}, new int[] {1}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void it_expects_no_exception_multiple_before_files() throws IOException {

    try {
      ExpectedFixes.checkExpectedFixLinesUsage(
          MULTIPLE_BEFORE_FILES_DIR, new int[] {}, new int[] {});
    } catch (final IllegalArgumentException ex) {
      fail("Exception not expected");
    }
  }

  @Test
  void it_expects_no_exception_fix_line_single_before_file() throws IOException {

    try {
      ExpectedFixes.checkExpectedFixLinesUsage(SINGLE_BEFORE_FILE_DIR, new int[] {1}, new int[] {});
    } catch (final IllegalArgumentException ex) {
      fail("Exception not expected");
    }
  }

  @Test
  void it_expects_no_exception_fail_fix_line_single_before_file() throws IOException {

    try {
      ExpectedFixes.checkExpectedFixLinesUsage(SINGLE_BEFORE_FILE_DIR, new int[] {}, new int[] {1});
    } catch (final IllegalArgumentException ex) {
      fail("Exception not expected");
    }
  }

  @Test
  void it_expects_no_exception_single_before_file() throws IOException {

    try {
      ExpectedFixes.checkExpectedFixLinesUsage(SINGLE_BEFORE_FILE_DIR, new int[] {}, new int[] {});
    } catch (final IllegalArgumentException ex) {
      fail("Exception not expected");
    }
  }
}
