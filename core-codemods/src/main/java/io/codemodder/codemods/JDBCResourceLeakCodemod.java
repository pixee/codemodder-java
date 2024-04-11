package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.codeql.CodeQLSarifJavaParserChanger;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import javax.inject.Inject;

/**
 * A codemod for automatically fixing JDBC resource leaks detected by CodeQL's rule
 * "java/database-resource-leak" whenever possible.
 */
@Codemod(
    id = "codeql:java/database-resource-leak",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class JDBCResourceLeakCodemod extends CodeQLSarifJavaParserChanger<MethodCallExpr> {

  @Inject
  public JDBCResourceLeakCodemod(
      @ProvidedCodeQLScan(ruleId = "java/database-resource-leak") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class, SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    return ResourceLeakFixer.checkAndFix(methodCallExpr).isPresent()
        ? ChangesResult.changesApplied
        : ChangesResult.noChanges;
  }

  @Override
  public DetectorRule getDetectorRule() {
    return new DetectorRule(
        "database-resource-leak",
        "Prevent database resource leaks (CodeQL)",
        "https://codeql.github.com/codeql-query-help/java/java-database-resource-leak/");
  }
}
