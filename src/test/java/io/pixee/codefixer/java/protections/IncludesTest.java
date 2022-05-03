package io.pixee.codefixer.java.protections;

import static io.pixee.codefixer.java.protections.WeavingNgTests.assertJavaWeaveWorkedAndWontReweave;
import static io.pixee.codefixer.java.protections.WeavingNgTests.scanAndAssertNoErrorsWithNoFilesChanged;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.pixee.codefixer.java.IncludesExcludes;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

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
    assertNoFindingsFoundWithIncludeExcludes(
        Collections.emptyList(),
        List.of("src/test/java/com/acme/testcode/RequestForwardVulnerability.java:16"));
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
    assertNoFindingsFoundWithSingleInclude("src/non_existent/");
  }

  @Test
  void it_rejects_matching_path_with_longer_exclude_path() throws IOException {
    assertNoFindingsFoundWithIncludeExcludes(List.of("src/test/"), List.of("src/test/java"));
  }

  @Test
  void it_honors_includes_matching_path_and_unmatching_line() throws IOException {
    assertNoFindingsFoundWithSingleInclude(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java:99");
  }

  private void assertNoFindingsFoundWithIncludeExcludes(
      final List<String> includes, final List<String> excludes) throws IOException {
    scanAndAssertNoErrorsWithNoFilesChanged(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactoryNg(),
        IncludesExcludes.fromConfiguration(new File(""), includes, excludes));
  }

  private void assertNoFindingsFoundWithSingleInclude(final String includePattern)
      throws IOException {
    scanAndAssertNoErrorsWithNoFilesChanged(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactoryNg(),
        IncludesExcludes.fromConfiguration(
            new File(""), List.of(includePattern), Collections.emptyList()));
  }

  private void assertItWorksWithWithIncludeExcludes(
      final List<String> includePatterns, final List<String> excludePatterns) throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactoryNg(),
        IncludesExcludes.fromConfiguration(new File(""), includePatterns, excludePatterns));
  }

  private void assertItWorksWithSingleInclude(final String includePattern) throws IOException {
    assertJavaWeaveWorkedAndWontReweave(
        "src/test/java/com/acme/testcode/RequestForwardVulnerability.java",
        new JakartaForwardVisitoryFactoryNg(),
        IncludesExcludes.fromConfiguration(
            new File(""), List.of(includePattern), Collections.emptyList()));
  }
}
