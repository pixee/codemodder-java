package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.codeql.CodeQLSarifJavaParserChanger;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.resourceleak.ResourceLeakFixer;
import javax.inject.Inject;

/**
 * A codemod for automatically fixing input resource leaks detected by CodeQL's rule
 * "java/input-resource-leak" whenever possible.
 */
@Codemod(
    id = "codeql:java/input-resource-leak",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLInputResourceLeakCodemod extends CodeQLSarifJavaParserChanger<Expression> {

  @Inject
  public CodeQLInputResourceLeakCodemod(
      @ProvidedCodeQLScan(ruleId = "java/input-resource-leak") final RuleSarif sarif) {
    super(sarif, Expression.class, SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Expression expr,
      final Result result) {
    return ResourceLeakFixer.checkAndFix(expr).isPresent()
        ? ChangesResult.changesApplied
        : ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "input-resource-leak",
        "Prevent resource leaks",
        "https://codeql.github.com/codeql-query-help/java/java-input-resource-leak/");
  }
}
