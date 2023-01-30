package io.openpixee.codetl.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pixee.codetf.CodeTFReport;
import io.github.pixee.codetf.CodeTFResult;
import io.openpixee.codetl.test.integration.junit.CodeTLExecutable;
import io.openpixee.codetl.test.integration.junit.CodeTLExecutableUnderTest;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

final class WebGoat822Test extends GitRepositoryTest {

  void setParameters() {
    this.repoURI = "https://github.com/WebGoat/WebGoat";
    this.repoBranch = "release/v8.2.2";
    this.tempDirName = "WebGoat822";
  }

  @Test
  void it_transforms_webgoat_normally(@CodeTLExecutableUnderTest final CodeTLExecutable codetl)
      throws Exception {
    // TODO integrate me with CodeTLExecutableExtension
    try {
      codetl.execute("-o", outputFile.getPath(), "-r", repoDir.getPath());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TestAbortedException("interrupted while waiting for codetl process", e);
    }

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    assertThat(report.getRun().getFailedFiles()).isEmpty();
    assertThat(report.getResults()).hasSize(21);

    // we only inject into a couple files
    assertThat(report.getResults().stream())
        .anyMatch(changedFile -> changedFile.getPath().endsWith("SerializationHelper.java"));
    assertThat(report.getResults().stream())
        .anyMatch(
            changedFile -> changedFile.getPath().endsWith("InsecureDeserializationTask.java"));

    // this file is only changed by including the codeql results, which we didn't do in this test
    assertThat(report.getResults().stream())
        .noneMatch(
            changedFile -> changedFile.getPath().endsWith("AjaxAuthenticationEntrypoint.java"));

    // and inject the correct pom
    var pomPath = "webgoat-lessons$insecure-deserialization$pom.xml".replace("$", File.separator);
    assertThat(report.getResults().stream())
        .anyMatch(changedFile -> changedFile.getPath().equals(pomPath));
  }

  @Test
  void it_transforms_webgoat_with_codeql(
      @TempDir final Path tmp, @CodeTLExecutableUnderTest final CodeTLExecutable codetl)
      throws Exception {
    final var filename =
        "webgoat_v8.2.0_codeql.sarif"; // TODO is this supposed to be webgoat_v8.2.2_contrast.sarif?
    final var sarif = tmp.resolve(filename);
    try (var is =
        Objects.requireNonNull(
            WebGoat820Test.class.getResourceAsStream("/" + filename),
            "Expected to find test SARIF file " + filename)) {
      Files.copy(is, sarif);
    }

    try {
      codetl.execute("-o", outputFile.getPath(), "-r", repoDir.getPath(), "-s", sarif.toString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TestAbortedException("interrupted while waiting for codetl process", e);
    }

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    assertThat(report.getRun().getFailedFiles()).hasSize(0);
    assertThat(report.getResults()).hasSize(24);

    // we only inject into a couple files
    assertThat(report.getResults().stream())
        .anyMatch(changedFile -> changedFile.getPath().endsWith("SerializationHelper.java"));
    assertThat(report.getResults().stream())
        .anyMatch(
            changedFile -> changedFile.getPath().endsWith("InsecureDeserializationTask.java"));

    // and inject the correct pom
    var pomPath = "webgoat-lessons$insecure-deserialization$pom.xml".replace("$", File.separator);
    assertThat(report.getResults().stream())
        .anyMatch(changedFile -> changedFile.getPath().equals(pomPath));

    // count the changes associated with missing-jwt-signature-check from codeql
    final var changes =
        report.getResults().stream()
            .map(CodeTFResult::getChanges)
            .flatMap(List::stream)
            .filter(
                change -> "codeql:java/missing-jwt-signature-check".equals(change.getCategory()));
    assertThat(changes).hasSize(6);

    // this file is also only changed by including the codeql results
    assertThat(report.getResults().stream())
        .anyMatch(
            changedFile ->
                changedFile.getPath().endsWith("AjaxAuthenticationEntryPoint.java")
                    && changedFile.getChanges().get(0).getLineNumber() == 53
                    && "codeql:java/stack-trace-exposure"
                        .equals(changedFile.getChanges().get(0).getCategory()));
  }
}
