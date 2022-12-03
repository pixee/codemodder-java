package io.openpixee.java.protections;

import static io.openpixee.java.protections.WeavingTests.assertJavaWeaveWorkedAndWontReweave;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.openpixee.java.IncludesExcludes;
import io.openpixee.java.LineIncludesExcludes;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * This class is a mix of integration and unit tests, and should probably move completely to unit
 * tests.
 */
final class IncludesTest {

  @Test
  void scans_all_by_default() throws IOException {
    assertItWorksWithWithIncludeExcludes(Collections.emptyList(), Collections.emptyList());
  }

  @Test
  void it_honors_includes_matching_directory() throws IOException {
    assertItWorksWithSingleInclude("src/test/java");
  }

  @Test
  void it_honors_matching_include_unmatching_exclude() throws IOException {
    assertItWorksWithWithIncludeExcludes(List.of("src/test/java"), List.of("src/non_existent"));
  }

  @Test
  void it_honors_multiple_matching_includes_unmatching_exclude() throws IOException {
    assertItWorksWithWithIncludeExcludes(
        List.of("src/test/java", "src/test"), List.of("src/test/NON_EXISTENT"));
  }

  @Test
  void it_honors_includes_matching_path() throws IOException {
    assertItWorksWithSingleInclude(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java");
  }

  @Test
  void it_honors_includes_path_and_line() throws IOException {
    assertItWorksWithSingleInclude(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java:16");
  }

  @Test
  void it_honors_excludes_path_and_line() throws IOException {
    IncludesExcludes includesExcludes =
        IncludesExcludes.fromConfiguration(
            new File("."),
            Collections.emptyList(),
            List.of("src/test/java/com/acme/testcode/RequestForwardVulnerability.java:16"));
    assertThat(
        includesExcludes.shouldInspect(
            new File("src/test/java/com/acme/testcode/RequestForwardVulnerability.java")),
        is(true));
    assertThat(includesExcludes.shouldInspect(new File("src/test/java/Anything.java")), is(true));

    // all lines are allowed on random files that match the include
    LineIncludesExcludes otherAllowedFile =
        includesExcludes.getIncludesExcludesForFile(new File("src/test/java/Anything.java"));
    assertThat(otherAllowedFile.matches(1), is(true));
    assertThat(otherAllowedFile.matches(16), is(true));
    assertThat(otherAllowedFile.matches(1000), is(true));

    // only line 16 is excluded in the file in question
    LineIncludesExcludes mostlyAllowedFile =
        includesExcludes.getIncludesExcludesForFile(
            new File("src/test/java/com/acme/testcode/RequestForwardVulnerability.java"));
    assertThat(mostlyAllowedFile.matches(1), is(true));
    assertThat(mostlyAllowedFile.matches(16), is(false));
    assertThat(mostlyAllowedFile.matches(1000), is(true));
  }

  @Test
  void it_blows_up_with_includes_and_excludes_specified_per_file() {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          assertItWorksWithWithIncludeExcludes(
              List.of("src/test/java/com/acme/testcode/RequestForwardVulnerability.java:16"),
              List.of("src/test/java/com/acme/testcode/RequestForwardVulnerability.java:17"));
        });
  }

  @Test
  void it_honors_unmatching_path() throws IOException {
    IncludesExcludes includesExcludes =
        IncludesExcludes.fromConfiguration(
            new File("src/non_existent/"), List.of("src/test/"), Collections.emptyList());
    assertThat(
        includesExcludes.shouldInspect(new File("src/non_existent/Anything.java")), is(false));
  }

  @Test
  void it_rejects_matching_path_with_longer_exclude_path() throws IOException {
    IncludesExcludes includesExcludes =
        IncludesExcludes.fromConfiguration(
            new File("."), List.of("src/test/"), List.of("src/test/java"));
    assertThat(includesExcludes.shouldInspect(new File("src/test/java/Foo.java")), is(false));
  }

  @Test
  void it_honors_includes_matching_path_and_unmatching_line() throws IOException {
    assertNoFindingsFoundWithSingleInclude(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java:99");

    IncludesExcludes includesExcludes =
        IncludesExcludes.fromConfiguration(
            new File("."),
            List.of("src/test/java/com/acme/testcode/RequestForwardVulnerability.java:99"),
            Collections.emptyList());
    assertThat(includesExcludes.shouldInspect(new File("src/test/java/Foo.java")), is(false));
  }

  private void assertNoFindingsFoundWithIncludeExcludes(
      final List<String> includes, final List<String> excludes) throws IOException {
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactory(),
        IncludesExcludes.fromConfiguration(new File("."), includes, excludes));
  }

  private void assertNoFindingsFoundWithSingleInclude(final String includePattern)
      throws IOException {
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactory(),
        IncludesExcludes.fromConfiguration(
            new File("."), List.of(includePattern), Collections.emptyList()));
  }

  private void assertItWorksWithWithIncludeExcludes(
      final List<String> includePatterns, final List<String> excludePatterns) throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactory(),
        IncludesExcludes.fromConfiguration(new File("."), includePatterns, excludePatterns));
  }

  private void assertItWorksWithSingleInclude(final String includePattern) throws IOException {
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactory(),
        IncludesExcludes.fromConfiguration(
            new File("."), List.of(includePattern), Collections.emptyList()));
  }
}
