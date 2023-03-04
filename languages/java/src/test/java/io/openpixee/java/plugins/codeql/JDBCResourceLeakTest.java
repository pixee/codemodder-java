package io.openpixee.java.plugins.codeql;

import io.openpixee.java.IncludesExcludes;
import io.openpixee.java.plugins.JavaSarifMockFactory;
import io.openpixee.java.protections.WeavingTests;
import java.io.File;
import java.io.IOException;
import java.util.Set;
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
            new File("."),
            Set.of(JavaSarifMockFactory.buildResult(insecureFilePath, 13, 16, 13, 38))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_does_nothing_to_stmt_because_rs_is_assigned_out_of_scope() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/resourceleak/JDBCNoFixRSAssignedOutOfScope.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."),
            Set.of(JavaSarifMockFactory.buildResult(insecureFilePath, 14, 18, 14, 40))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_does_nothing_to_stmt_because_rs_is_assigned_to_a_field() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/resourceleak/JDBCNoFixRSAssignedToAField.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."),
            Set.of(JavaSarifMockFactory.buildResult(insecureFilePath, 14, 16, 14, 38))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_does_nothing_to_stmt_because_rs_is_a_argument() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/resourceleak/JDBCNoFixRSParameter.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."),
            Set.of(JavaSarifMockFactory.buildResult(insecureFilePath, 19, 16, 19, 38))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_does_nothing_to_rs_because_stmt_leaks() throws IOException {
    String insecureFilePath =
        "src/test/java/com/acme/testcode/resourceleak/JDBCNoFixRSLeakByStmt.java";
    WeavingTests.scanAndAssertNoErrorsWithNoFilesChanged(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."),
            Set.of(JavaSarifMockFactory.buildResult(insecureFilePath, 14, 20, 14, 44))),
        new IncludesExcludes.MatchesEverything());
  }

  @Test
  void it_wraps_stmt_as_a_try_resource() throws IOException {
    String insecureFilePath = "src/test/java/com/acme/testcode/resourceleak/JDBCSimpleLeak.java";
    WeavingTests.assertJavaWeaveWorkedAndWontReweave(
        insecureFilePath,
        new JDBCResourceLeakVisitorFactory(
            new File("."),
            Set.of(JavaSarifMockFactory.buildResult(insecureFilePath, 13, 22, 13, 44))),
        new IncludesExcludes.MatchesEverything());
  }
}
