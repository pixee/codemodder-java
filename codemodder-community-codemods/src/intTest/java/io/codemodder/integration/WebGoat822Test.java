package io.codemodder.integration;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.codemods.DefaultCodemods;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFResult;
import java.io.FileReader;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class WebGoat822Test extends GitRepositoryTest {

  WebGoat822Test() {
    super("https://github.com/WebGoat/WebGoat", "main", "e75cfbeb110e3d3a2ca3c8fee2754992d89c419d");
  }

  @Test
  void it_injects_dependency_even_when_no_poms_included() throws Exception {
    DefaultCodemods.main(
        new String[] {
          "--path-include",
          "**/InsecureDeserializationTask.java",
          "--output",
          outputFile.getPath(),
          "--dont-exit",
          repoDir.getPath()
        });

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    verifyNoFailedFiles(report);
    List<CodeTFResult> results = report.getResults();
    assertThat(results.size(), is(1));
    CodeTFResult result = results.get(0);
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size(), is(2));
    assertThat(
        changeset.get(0).getPath(),
        equalTo(
            "webgoat-lessons/insecure-deserialization/src/main/java/org/owasp/webgoat/deserialization/InsecureDeserializationTask.java"));
    assertThat(
        changeset.get(1).getPath(), equalTo("webgoat-lessons/insecure-deserialization/pom.xml"));
  }

  @Test
  void it_transforms_webgoat_normally() throws Exception {
    DefaultCodemods.main(
        new String[] {"--output", outputFile.getPath(), "--dont-exit", repoDir.getPath()});

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    verifyNoFailedFiles(report);

    List<CodeTFChangesetEntry> fileChanges =
        report.getResults().stream()
            .map(CodeTFResult::getChangeset)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    assertThat(fileChanges.size(), is(23));

    // we only inject into a couple files
    verifyStandardCodemodResults(fileChanges);

    // this file is only changed by including the codeql results, which we didn't do in this test
    assertThat(
        fileChanges.stream()
            .map(CodeTFChangesetEntry::getPath)
            .anyMatch(path -> path.endsWith("AjaxAuthenticationEntryPoint.java")),
        is(false));
  }

  @Test
  void it_transforms_webgoat_with_codeql() throws Exception {

    DefaultCodemods.main(
        new String[] {
          "--output",
          outputFile.getPath(),
          "--sarif",
          "src/test/resources/webgoat_v8.2.2_codeql.sarif",
          "--dont-exit",
          repoDir.getPath()
        });

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    verifyNoFailedFiles(report);

    List<CodeTFChangesetEntry> fileChanges =
        report.getResults().stream()
            .map(CodeTFResult::getChangeset)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    assertThat(fileChanges.size(), is(28));

    verifyStandardCodemodResults(fileChanges);

    // count the changes associated with missing-jwt-signature-check from codeql
    List<CodeTFResult> jwtResults =
        report.getResults().stream()
            .filter(result -> "codeql:java/missing-jwt-signature-check".equals(result.getCodemod()))
            .collect(Collectors.toList());
    assertThat(jwtResults.size(), equalTo(1));

    // this file is also only changed by including the codeql results
    CodeTFChangesetEntry ajaxJwtChange =
        fileChanges.stream()
            .filter(change -> change.getPath().endsWith("AjaxAuthenticationEntryPoint.java"))
            .findFirst()
            .orElseThrow();

    assertThat(ajaxJwtChange.getChanges().size(), equalTo(1));
    assertThat(ajaxJwtChange.getChanges().get(0).getLineNumber(), equalTo(53));
  }

  private static void verifyStandardCodemodResults(final List<CodeTFChangesetEntry> fileChanges) {
    // we only inject into a couple files
    assertThat(
        fileChanges.stream()
            .map(CodeTFChangesetEntry::getPath)
            .anyMatch(path -> path.endsWith("SerializationHelper.java")),
        is(true));

    assertThat(
        fileChanges.stream()
            .map(CodeTFChangesetEntry::getPath)
            .anyMatch(path -> path.endsWith("InsecureDeserializationTask.java")),
        is(true));

    // and inject the correct pom

    assertThat(
        fileChanges.stream()
            .map(CodeTFChangesetEntry::getPath)
            .anyMatch(path -> path.equals("webgoat-lessons/insecure-deserialization/pom.xml")),
        is(true));
  }

  private static void verifyNoFailedFiles(final CodeTFReport report) {
    List<String> failedFiles =
        report.getResults().stream()
            .map(CodeTFResult::getFailedFiles)
            .flatMap(Collection::stream)
            .collect(Collectors.toList());
    assertThat(failedFiles.size(), is(0));
  }
}