package io.openpixee.java.plugins.codeql;

import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import io.openpixee.java.IncludesExcludes;
import io.openpixee.java.protections.WeavingTests;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests relating to JEXL injections detected by CodeQl. See @see <a
 * href=""https://codeql.github.com/codeql-query-help/java/java-jexl-expression-injection/>CodeQL -
 * Expression language injection</a>.
 */
final class JEXLInjectionTest {

  @Test
  void it_does_nothing_because_of_no_results_in_sarif() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/jexlinjection/JEXLInjectionSimple.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JEXLInjectionVisitorFactory(new File("."), Collections.emptySet()),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_does_nothing_as_builder_is_not_local() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/jexlinjection/JEXLInjectionNoFix.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JEXLInjectionVisitorFactory(
            new File("."), Set.of(buildResult(insecureFilePath, 21, 7, 21, 17))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_sandboxes_jexl_injection_immediate() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/jexlinjection/JEXLInjectionImmediate.java";
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        insecureFilePath,
        new JEXLInjectionVisitorFactory(
            new File("."), Set.of(buildResult(insecureFilePath, 19, 7, 19, 57))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_sandboxes_jexl_injection() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/jexlinjection/JEXLInjectionSimple.java";
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        insecureFilePath,
        new JEXLInjectionVisitorFactory(
            new File("."), Set.of(buildResult(insecureFilePath, 23, 7, 23, 17))),
        new IncludesExcludes.MatchesEverything());
  }

  private static Result buildResult(
      final String insecureFilePath,
      final int startLine,
      final int startColumn,
      final int endLine,
      final int endColumn) {
    return new Result()
        .withLocations(
            List.of(buildLocation(insecureFilePath, startLine, startColumn, endLine, endColumn)));
  }

  private static Location buildLocation(
      final String insecureFilePath,
      final int startLine,
      final int startColumn,
      final int endLine,
      final int endColumn) {
    return new Location()
        .withPhysicalLocation(
            new PhysicalLocation()
                .withRegion(
                    new Region()
                        .withStartLine(startLine)
                        .withEndLine(endLine)
                        .withStartColumn(startColumn)
                        .withEndColumn(endColumn))
                .withArtifactLocation(new ArtifactLocation().withUri(insecureFilePath)));
  }
}
