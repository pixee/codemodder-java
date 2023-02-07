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
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Tests relating to JDBC database resource leaks detected by CodeQl. See @see <a
 * href="https://codeql.github.com/codeql-query-help/java/java-database-resource-leak/">CodeQL -
 * Potential database resource leak </a>.
 */
final class JDBCResourceLeakTest {

  @Test
  void it_does_nothing_to_stmt_because_rs_returns() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/resourceleak/JDBCNoFixReturningResultSet.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."), Set.of(buildResult(insecureFilePath, 13))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_does_nothing_to_stmt_because_rs_is_assigned_out_of_scope() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/resourceleak/JDBCNoFixRSAssignedOutOfScope.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."), Set.of(buildResult(insecureFilePath, 13))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_does_nothing_to_stmt_because_rs_is_assigned_to_a_field() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/resourceleak/JDBCNoFixRSAssignedToAField.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."), Set.of(buildResult(insecureFilePath, 13))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_does_nothing_to_stmt_because_rs_is_a_argument() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/resourceleak/JDBCNoFixRSParameter.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."), Set.of(buildResult(insecureFilePath, 13))),
        new IncludesExcludes.MatchesEverything());
  }

  private Result buildResult(final String insecureFilePath, final int i) {
    return new Result().withLocations(List.of(buildLocation(insecureFilePath, i)));
  }

  private Location buildLocation(final String insecureFilePath, final int line) {
    return new Location()
        .withPhysicalLocation(
            new PhysicalLocation()
                .withRegion(new Region().withStartLine(line))
                .withArtifactLocation(new ArtifactLocation().withUri(insecureFilePath)));
  }
}
