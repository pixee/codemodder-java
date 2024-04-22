package io.codemodder.testutils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import org.junit.jupiter.api.Test;

final class MetadataUtilTest {

  @Test
  void it_expects_exception_fixes_multiple_before_files() {

    assertThatThrownBy(
            () ->
                MetadataUtil.checkExpectedFixLinesUsage(
                    "multiple-before-files", new int[] {1}, new int[] {}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void it_expects_exception_failed_fixes_multiple_before_files() {

    assertThatThrownBy(
            () ->
                MetadataUtil.checkExpectedFixLinesUsage(
                    "multiple-before-files", new int[] {}, new int[] {1}))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void it_expects_no_exception_multiple_before_files() throws IOException {

    try {
      MetadataUtil.checkExpectedFixLinesUsage("multiple-before-files", new int[] {}, new int[] {});
    } catch (final IllegalArgumentException ex) {
      fail("Exception not expected");
    }
  }

  @Test
  void it_expects_no_exception_fix_line_single_before_file() throws IOException {

    try {
      MetadataUtil.checkExpectedFixLinesUsage("single-before-file", new int[] {1}, new int[] {});
    } catch (final IllegalArgumentException ex) {
      fail("Exception not expected");
    }
  }

  @Test
  void it_expects_no_exception_fail_fix_line_single_before_file() throws IOException {

    try {
      MetadataUtil.checkExpectedFixLinesUsage("single-before-file", new int[] {}, new int[] {1});
    } catch (final IllegalArgumentException ex) {
      fail("Exception not expected");
    }
  }

  @Test
  void it_expects_no_exception_single_before_file() throws IOException {

    try {
      MetadataUtil.checkExpectedFixLinesUsage("single-before-file", new int[] {}, new int[] {});
    } catch (final IllegalArgumentException ex) {
      fail("Exception not expected");
    }
  }
}
