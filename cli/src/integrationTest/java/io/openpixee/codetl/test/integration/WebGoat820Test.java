package io.openpixee.codetl.test.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.pixee.codetf.CodeTFReport;
import io.github.pixee.codetf.CodeTFResult;
import io.openpixee.codetl.test.integration.junit.CloneRepository;
import io.openpixee.codetl.test.integration.junit.CodeTLExecutable;
import io.openpixee.codetl.test.integration.junit.CodeTLExecutableUnderTest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

/** Integration tests that run CodeTL with WebGoat 8.2.0. */
final class WebGoat820Test {

  @Test
  void it_transforms_webgoat_with_codeql(
      @CloneRepository(repo = "https://github.com/WebGoat/WebGoat", branch = "release/v8.2.0")
          final Path webgoat,
      @TempDir final Path tmp,
      @CodeTLExecutableUnderTest final CodeTLExecutable codetl)
      throws IOException {
    final var filename = "webgoat_v8.2.0_codeql.sarif";
    final var sarif = tmp.resolve(filename);
    try (var is = WebGoat820Test.class.getResourceAsStream("/" + filename)) {
      if (is == null) {
        throw new IllegalStateException("Expected to find test SARIF file " + filename);
      }
      Files.copy(is, sarif);
    }
    final Path output = tmp.resolve("output.codetf.json");

    try {
      codetl.execute("-o", output.toString(), "-r", webgoat.toString(), "-s", sarif.toString());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TestAbortedException("interrupted while waiting for codetl process", e);
    }

    final CodeTFReport report;
    try (var reader = Files.newBufferedReader(output)) {
      report = new ObjectMapper().readValue(reader, CodeTFReport.class);
    }

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
    assertThat(report.getResults().stream())
        .anyMatch(
            changedFile ->
                changedFile.getPath().endsWith("AjaxAuthenticationEntryPoint.java")
                    && changedFile.getChanges().get(0).getLineNumber() == 53
                    && "codeql:java/stack-trace-exposure"
                        .equals(changedFile.getChanges().get(0).getCategory()));

    // we only check our pom injection for a couple files
    assertThat(report.getResults().stream())
        .anyMatch(changedFile -> changedFile.getPath().endsWith("SerializationHelper.java"));
    assertThat(report.getResults().stream())
        .anyMatch(
            changedFile -> changedFile.getPath().endsWith("InsecureDeserializationTask.java"));

    // and inject the correct pom
    var pomPath = "webgoat-lessons$insecure-deserialization$pom.xml".replace("$", File.separator);
    assertThat(report.getResults().stream())
        .anyMatch(changedFile -> changedFile.getPath().equals(pomPath));
  }
}
