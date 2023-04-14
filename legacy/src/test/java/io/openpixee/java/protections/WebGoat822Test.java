package io.openpixee.java.protections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFResult;
import io.openpixee.java.JavaFixitCli;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

final class WebGoat822Test extends GitRepositoryTest {

  WebGoat822Test() {
    super("https://github.com/WebGoat/WebGoat", "main", "e75cfbeb110e3d3a2ca3c8fee2754992d89c419d");
  }

  @Test
  void it_injects_dependency_even_when_no_poms_included() throws Exception {
    int exitCode =
        new CommandLine(new JavaFixitCli())
            .execute(
                "--include", "**/InsecureDeserializationTask.java",
                "-o", outputFile.getPath(),
                "-r", repoDir.getPath());

    assertThat(exitCode, is(0));

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    assertThat(report.getRun().getFailedFiles().size(), is(0));
    List<CodeTFResult> results = report.getResults();
    assertThat(results.size(), is(2));

    // we only inject into a couple files
    assertThat(
        results.stream()
            .anyMatch(
                changedFile -> changedFile.getPath().endsWith("InsecureDeserializationTask.java")),
        is(true));

    // and inject the correct pom
    var pomPath = "webgoat-lessons$insecure-deserialization$pom.xml".replace("$", File.separator);
    assertThat(
        results.stream().anyMatch(changedFile -> changedFile.getPath().equals(pomPath)), is(true));
  }

  @Test
  void it_transforms_webgoat_normally() throws Exception {
    int exitCode =
        new CommandLine(new JavaFixitCli())
            .execute("-o", outputFile.getPath(), "-r", repoDir.getPath());

    assertThat(exitCode, is(0));

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    assertThat(report.getRun().getFailedFiles().size(), is(0));
    assertThat(report.getResults().size(), is(22));

    // we only inject into a couple files
    assertThat(
        report.getResults().stream()
            .anyMatch(changedFile -> changedFile.getPath().endsWith("SerializationHelper.java")),
        is(true));
    assertThat(
        report.getResults().stream()
            .anyMatch(
                changedFile -> changedFile.getPath().endsWith("InsecureDeserializationTask.java")),
        is(true));

    // this file is only changed by including the codeql results, which we didn't do in this test
    assertThat(
        report.getResults().stream()
            .anyMatch(
                changedFile -> changedFile.getPath().endsWith("AjaxAuthenticationEntrypoint.java")),
        is(false));

    // and inject the correct pom
    var pomPath = "webgoat-lessons$insecure-deserialization$pom.xml".replace("$", File.separator);
    assertThat(
        report.getResults().stream().anyMatch(changedFile -> changedFile.getPath().equals(pomPath)),
        is(true));
  }

  @Test
  @Disabled
  void it_transforms_webgoat_with_codeql() throws Exception {
    int exitCode =
        new CommandLine(new JavaFixitCli())
            .execute(
                "-o",
                outputFile.getPath(),
                "-r",
                repoDir.getPath(),
                "-s",
                "src/test/resources/webgoat_v8.2.0_codeql.sarif");

    assertThat(exitCode, is(0));

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    assertThat(report.getRun().getFailedFiles().size(), is(0));
    assertThat(report.getResults().size(), is(25));

    // we only inject into a couple files
    assertThat(
        report.getResults().stream()
            .anyMatch(changedFile -> changedFile.getPath().endsWith("SerializationHelper.java")),
        is(true));
    assertThat(
        report.getResults().stream()
            .anyMatch(
                changedFile -> changedFile.getPath().endsWith("InsecureDeserializationTask.java")),
        is(true));

    // and inject the correct pom
    var pomPath = "webgoat-lessons$insecure-deserialization$pom.xml".replace("$", File.separator);
    assertThat(
        report.getResults().stream().anyMatch(changedFile -> changedFile.getPath().equals(pomPath)),
        is(true));

    // count the changes associated with missing-jwt-signature-check from codeql
    List<CodeTFChange> changes =
        report.getResults().stream()
            .map(CodeTFResult::getChanges)
            .flatMap(List::stream)
            .filter(
                change -> "codeql:java/missing-jwt-signature-check".equals(change.getCategory()))
            .collect(Collectors.toUnmodifiableList());
    assertThat(changes.size(), equalTo(6));

    // this file is also only changed by including the codeql results
    assertThat(
        report.getResults().stream()
            .anyMatch(
                changedFile ->
                    changedFile.getPath().endsWith("AjaxAuthenticationEntryPoint.java")
                        && changedFile.getChanges().get(0).getLineNumber() == 53
                        && "codeql:java/stack-trace-exposure"
                            .equals(changedFile.getChanges().get(0).getCategory())),
        is(true));
  }
}
