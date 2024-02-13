package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
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
public final class JDBCResourceLeakCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public JDBCResourceLeakCodemod(
      @ProvidedCodeQLScan(ruleId = "java/database-resource-leak") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class, SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    return ResourceLeakFixer.checkAndFix(methodCallExpr).isPresent();
  }
}
