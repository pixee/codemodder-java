package io.openpixee.codetl.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pixee.codetf.CodeTFReport;
import io.github.pixee.codetf.CodeTFResult;
import io.openpixee.codetl.test.integration.junit.CodeTLExecutable;
import io.openpixee.codetl.test.integration.junit.CodeTLExecutableUnderTest;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

final class WebGoat820Test extends GitRepositoryTest {

  void setParameters() {
    this.repoURI = "https://github.com/WebGoat/WebGoat";
    this.repoBranch = "release/v8.2.0";
    this.tempDirName = "WebGoat820";
  }

  @Test
  void it_transforms_webgoat_with_codeql(
      @TempDir final Path tmp, @CodeTLExecutableUnderTest final CodeTLExecutable codetl)
      throws IOException {
    final var filename = "webgoat_v8.2.0_codeql.sarif";
    final var sarif = tmp.resolve(filename);
    try (var is = WebGoat820Test.class.getResourceAsStream("/" + filename)) {
      if (is == null) {
        throw new IllegalStateException("Expected to find test SARIF file " + filename);
      }
      Files.copy(is, sarif);
    }

    try {
      codetl.execute("-o", outputFile.getPath(), "-r", repoDir.getPath(), "-s", sarif.toString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TestAbortedException("interrupted while waiting for codetl process", e);
    }

    var report = new ObjectMapper().readValue(new FileReader(outputFile), CodeTFReport.class);

    assertThat(report.getRun().getFailedFiles()).isEmpty();
    assertThat(report.getResults()).hasSize(24);

    // count the changes associated with missing-jwt-signature-check from codeql
    var changes =
        report.getResults().stream()
            .map(CodeTFResult::getChanges)
            .flatMap(List::stream)
            .filter(
                change -> "codeql:java/missing-jwt-signature-check".equals(change.getCategory()));
    assertThat(changes).hasSize(6);

    // this file is also only changed by including the codeql results
    assertThat(
        report.getResults().stream())
        .anyMatch(
            changedFile ->
                changedFile.getPath().endsWith("AjaxAuthenticationEntryPoint.java")
                    && changedFile.getChanges().get(0).getLineNumber() == 53
                    && "codeql:java/stack-trace-exposure"
                    .equals(changedFile.getChanges().get(0).getCategory()));

    // we only check our pom injection for a couple files
    assertThat(
        report.getResults().stream())
        .anyMatch(changedFile -> changedFile.getPath().endsWith("SerializationHelper.java"));
    assertThat(
        report.getResults().stream())
        .anyMatch(
            changedFile -> changedFile.getPath().endsWith("InsecureDeserializationTask.java"));

    // and inject the correct pom
    var pomPath = "webgoat-lessons$insecure-deserialization$pom.xml".replace("$", File.separator);
    assertThat(
        report.getResults().stream()).anyMatch(
        changedFile -> changedFile.getPath().equals(pomPath));
  }
}
