package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.providers.sarif.codeql.CodeQLScan;
import javax.inject.Inject;

/**
 * A codemod for automatically fixing input resource leaks detected by CodeQL's rule
 * "java/input-resource-leak" whenever possible.
 */
@Codemod(
    id = "codeql:java/input-resource-leak",
    author = "andre.silva@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class InputResourceLeakCodemod extends SarifPluginJavaParserChanger<Expression> {

  @Inject
  public InputResourceLeakCodemod(
      @CodeQLScan(ruleId = "java/input-resource-leak") final RuleSarif sarif) {
    super(sarif, Expression.class, RegionExtractor.FROM_FIRST_LOCATION);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Expression expr,
      final Result result) {
    return ResourceLeakFixer.checkAndFix(expr).isPresent();
  }
}