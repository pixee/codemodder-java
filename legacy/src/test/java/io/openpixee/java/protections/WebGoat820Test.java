package io.openpixee.java.protections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFReport;
import io.openpixee.java.JavaFixitCli;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

final class WebGoat820Test extends GitRepositoryTest {

  WebGoat820Test() {
    super("https://github.com/WebGoat/WebGoat", "main", "486b81f8ecacf0f751e4a1dd873b6b09545cb2a2");
  }

  @Test
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

    // count the changes associated with missing-jwt-signature-check from codeql
    List<CodeTFChange> changes =
        report.getResults().stream()
            .map(CodeTFChangesetEntry::getChanges)
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

    // we only check our pom injection for a couple files
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
  }
}
