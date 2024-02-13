package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import io.codemodder.*;
import io.codemodder.providers.sarif.pmd.PmdScan;
import java.util.Set;
import javax.inject.Inject;

/**
 * A codemod for automatically switching the order of literals and variables in comparisons so
 * they're guaranteed not to throw {@link NullPointerException} when the variable is unexpectedly
 * null.
 */
@Codemod(
    id = "pixee:java/switch-literal-first",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SwitchLiteralFirstComparisonsCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public SwitchLiteralFirstComparisonsCodemod(
      @PmdScan(ruleId = "category/java/bestpractices.xml/LiteralsFirstInComparisons")
          final RuleSarif ruleSarif) {
    super(ruleSarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    // some of the methods that this rule applies to are not flippable, like compareTo() would
    // change the logic after
    if (!flippableComparisonMethods.contains(methodCallExpr.getNameAsString())) {
      return false;
    }
    Expression leftSide = methodCallExpr.getScope().get();
    Expression rightSide = methodCallExpr.getArgument(0);
    try {
      ResolvedType leftType = leftSide.calculateResolvedType();
      if ("Ljava/lang/String;".equals(leftType.toDescriptor())) {
        methodCallExpr.setScope(rightSide);
        methodCallExpr.setArgument(0, leftSide);
        return true;
      }
    } catch (UnsolvedSymbolException e) {
      // expected in cases where we can't resolve the type
    }

    return false;
  }

  private static final Set<String> flippableComparisonMethods =
      Set.of("equals", "equalsIgnoreCase");
}
