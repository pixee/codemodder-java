package io.codemodder.integration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import io.codemodder.codemods.DefaultCodemods;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFResult;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mozilla.universalchardet.UniversalDetector;

final class WebGoat822Test extends GitRepositoryTest {

  WebGoat822Test() {
    super("https://github.com/WebGoat/WebGoat", "main", "e75cfbeb110e3d3a2ca3c8fee2754992d89c419d");
  }

  @Test
  void it_injects_dependency_even_when_no_poms_included() throws Exception {

    // save original pom contents
    Path modulePom = repoDir.toPath().resolve("webgoat-lessons/insecure-deserialization/pom.xml");
    assertThat(Files.exists(modulePom), is(true));
    String originalModulePomContents = Files.readString(modulePom);
    String encoding = UniversalDetector.detectCharset(modulePom);
    List<String> originalPomContentLines = Files.readAllLines(modulePom);

    DefaultCodemods.main(
        new String[] {
          "--path-include",
          "**/InsecureDeserializationTask.java",
          "--output",
          outputFile.getPath(),
          "--dont-exit",
          repoDir.getPath()
        });

    ObjectMapper objectMapper = new ObjectMapper();

    var report = objectMapper.readValue(new FileReader(outputFile), CodeTFReport.class);

    verifyNoFailedFiles(report);
    List<CodeTFResult> results = report.getResults();
    CodeTFResult result =
        results.stream()
            .filter(r -> r.getCodemod().equals("pixee:java/harden-java-deserialization"))
            .findFirst()
            .orElseThrow();
    List<CodeTFChangesetEntry> changeset = result.getChangeset();
    assertThat(changeset.size(), is(3));
    assertThat(
        changeset.get(0).getPath(),
        equalTo(
            "webgoat-lessons/insecure-deserialization/src/main/java/org/owasp/webgoat/deserialization/InsecureDeserializationTask.java"));
    assertThat(
        changeset.get(1).getPath(), equalTo("webgoat-lessons/insecure-deserialization/pom.xml"));

    // verify that we can apply the pom diff back to the original pom as a patch
    String newModulePomContents = Files.readString(modulePom);
    assertThat(newModulePomContents, not(equalTo(originalModulePomContents)));
    String diff = changeset.get(1).getDiff();
    List<String> pomPatchContents = diff.lines().toList();
    Patch<String> pomPatch = UnifiedDiffUtils.parseUnifiedDiff(pomPatchContents);
    List<String> newPomContentLines = DiffUtils.patch(originalPomContentLines, pomPatch);
    assertThat(String.join("\n", newPomContentLines), equalTo(newModulePomContents.trim()));

    String afterEncoding = UniversalDetector.detectCharset(modulePom);
    assertThat(encoding, equalTo(afterEncoding));
  }

  @Test
  void it_transforms_webgoat_normally() throws Exception {
    DefaultCodemods.main(
        new String[] {
          "--output",
          outputFile.getPath(),
          "--verbose",
          "--dont-exit",
          repoDir.getPath(),
          "--path-include=" + testPathIncludes,
          "--path-exclude=" + testPathExcludes,
        });

    ObjectMapper objectMapper = new ObjectMapper();

    var report = objectMapper.readValue(new FileReader(outputFile), CodeTFReport.class);

    verifyNoFailedFiles(report);

    List<CodeTFChangesetEntry> fileChanges =
        report.getResults().stream()
            .map(CodeTFResult::getChangeset)
            .flatMap(Collection::stream)
            .toList();

    assertThat(fileChanges.size(), is(58));

    // we only inject into a couple files
    verifyStandardCodemodResults(fileChanges);

    // and that we inject the correct pom
    assertThat(
        fileChanges.stream()
            .map(CodeTFChangesetEntry::getPath)
            .anyMatch(path -> path.endsWith("insecure-deserialization/pom.xml")),
        is(true));

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
          "--path-include=" + testPathIncludes,
          "--path-exclude=" + testPathExcludes,
          repoDir.getPath()
        });

    ObjectMapper objectMapper = new ObjectMapper();

    var report = objectMapper.readValue(new FileReader(outputFile), CodeTFReport.class);

    verifyNoFailedFiles(report);

    List<CodeTFChangesetEntry> fileChanges =
        report.getResults().stream()
            .map(CodeTFResult::getChangeset)
            .flatMap(Collection::stream)
            .toList();

    assertThat(fileChanges.size(), is(64));

    verifyStandardCodemodResults(fileChanges);

    // count the changes associated with missing-jwt-signature-check from codeql
    List<CodeTFResult> jwtResults =
        report.getResults().stream()
            .filter(result -> "codeql:java/missing-jwt-signature-check".equals(result.getCodemod()))
            .toList();
    assertThat(jwtResults.size(), equalTo(1));

    // this file is also only changed by including the codeql results
    CodeTFChangesetEntry ajaxJwtChange =
        fileChanges.stream()
            .filter(change -> change.getPath().endsWith("AjaxAuthenticationEntryPoint.java"))
            .findFirst()
            .orElseThrow();

    assertThat(ajaxJwtChange.getChanges().size(), equalTo(1));
    assertThat(ajaxJwtChange.getChanges().get(0).getLineNumber(), equalTo(53));

    verifyCodemodsHitWithChangesetCount(report, "codeql:java/insecure-randomness", 0);
    verifyCodemodsHitWithChangesetCount(report, "codeql:java/ssrf", 3);
    verifyCodemodsHitWithChangesetCount(report, "codeql:java/sql-injection", 5);
    verifyCodemodsHitWithChangesetCount(report, "codeql:java/insecure-cookie", 1);
  }
}
