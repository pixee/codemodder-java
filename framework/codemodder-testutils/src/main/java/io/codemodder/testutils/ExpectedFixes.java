package io.codemodder.testutils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import io.codemodder.CodeChanger;
import io.codemodder.FixOnlyCodeChanger;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFResult;
import io.codemodder.codetf.UnfixedFinding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Utilities for verifying expected fixes. */
final class ExpectedFixes {

  private ExpectedFixes() {}

  /** Verify the expected fix metadata. */
  static void verifyExpectedFixes(
      final Path testResourceDir,
      final CodeTFResult result,
      final CodeChanger changer,
      final int[] expectedFixLines,
      final int[] expectingFailedFixesAtLines)
      throws IOException {
    ExpectedFixes.checkExpectedFixLinesUsage(
        testResourceDir, expectedFixLines, expectingFailedFixesAtLines);

    List<CodeTFChange> changes =
        result.getChangeset().stream()
            .map(CodeTFChangesetEntry::getChanges)
            .flatMap(List::stream)
            .toList();

    if (changer instanceof FixOnlyCodeChanger) {
      assertThat(changes.stream().anyMatch(c -> !c.getFixedFindings().isEmpty()), is(true));
    }

    for (int expectedFixLine : expectedFixLines) {
      assertThat(changes.stream().anyMatch(c -> c.getLineNumber() == expectedFixLine), is(true));
    }

    List<UnfixedFinding> unfixedFindings = result.getUnfixedFindings();
    for (int expectedFailedFixLine : expectingFailedFixesAtLines) {
      assertThat(
          unfixedFindings.stream().noneMatch(c -> c.getLine() == expectedFailedFixLine), is(true));
    }
  }

  static void checkExpectedFixLinesUsage(
      final Path testResourceDir,
      final int[] expectingFixesAtLines,
      final int[] expectingFailedFixesAtLines)
      throws IOException {

    try (var stream = Files.walk(testResourceDir)) {
      List<Path> paths =
          stream
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
}
