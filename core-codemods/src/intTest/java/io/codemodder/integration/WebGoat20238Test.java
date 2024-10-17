package io.codemodder.integration;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.codemods.DefaultCodemods;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFReport;
import io.codemodder.codetf.CodeTFResult;
import java.io.FileReader;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

final class WebGoat20238Test extends GitRepositoryTest {

  WebGoat20238Test() {
    super("https://github.com/WebGoat/WebGoat", "main", "f9b810c5ee2d6731eb5e37172af20d276a7dfb98");
  }

  @Test
  void it_remediates_webgoat_2023_8() throws Exception {

    DefaultCodemods.main(
        new String[] {
          "--output",
          outputFile.getPath(),
          "--sarif",
          "src/test/resources/webgoat_v2023.8_from_ghas_06_2024.sarif",
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

    assertThat(fileChanges.size(), is(50));

    verifyStandardCodemodResults(fileChanges);

    // count the changes associated with missing-jwt-signature-check from codeql
    List<CodeTFResult> jwtResults =
        report.getResults().stream()
            .filter(result -> "codeql:java/missing-jwt-signature-check".equals(result.getCodemod()))
            .toList();
    assertThat(jwtResults.size(), equalTo(1));

    // this file is also only changed by including the codeql results
    CodeTFChangesetEntry jwtChange =
        fileChanges.stream()
            .filter(change -> change.getPath().endsWith("JWTRefreshEndpoint.java"))
            .findFirst()
            .orElseThrow();

    assertThat(jwtChange.getChanges().size(), equalTo(2));
    assertThat(jwtChange.getChanges().get(0).getLineNumber(), equalTo(113));
    assertThat(jwtChange.getChanges().get(1).getLineNumber(), equalTo(140));

    verifyCodemodsHitWithChangesetCount(report, "codeql:java/insecure-randomness", 0);
    verifyCodemodsHitWithChangesetCount(report, "codeql:java/ssrf", 1);
    verifyCodemodsHitWithChangesetCount(report, "codeql:java/xxe", 1);
    verifyCodemodsHitWithChangesetCount(report, "codeql:java/sql-injection", 6);
    verifyCodemodsHitWithChangesetCount(report, "codeql:java/insecure-cookie", 2);
  }
}
