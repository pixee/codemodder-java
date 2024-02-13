package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/** Turns hardcoded seeds for PRNGs to be more random. */
@Codemod(
    id = "pixee:java/make-prng-seed-unpredictable",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class RandomizeSeedCodemod extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public RandomizeSeedCodemod(
      @SemgrepScan(ruleId = "make-prng-seed-unpredictable") final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr setSeedCall,
      final Result result) {
    MethodCallExpr safeExpression =
        new MethodCallExpr(new NameExpr(System.class.getSimpleName()), "currentTimeMillis");
    NodeList<Expression> arguments = setSeedCall.getArguments();
    arguments.set(0, safeExpression);
    return true;
  }
}
