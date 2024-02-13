package io.codemodder.codemods;

import static io.codemodder.javaparser.ASTExpectations.expect;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import java.util.Optional;
import javax.inject.Inject;

/** Fix unsafe NIO path comparison. */
@Codemod(
    id = "pixee:java/fix-unsafe-nio-path-comparison",
    importance = Importance.MEDIUM,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class FixUnsafeNIOPathComparisonCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  private static final String RULE =
      """
          rules:
            - id: fix-unsafe-nio-path-comparison
              patterns:
                - pattern: (File $CHILD).getCanonicalPath().startsWith((File $PARENT).getCanonicalPath())
          """;

  @Inject
  public FixUnsafeNIOPathComparisonCodemod(@SemgrepScan(yaml = RULE) final RuleSarif sarif) {
    super(sarif, MethodCallExpr.class);
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {

    // make sure we have the event for the startsWith() call, not the getCanonicalPath() calls
    if (!"startsWith".equals(methodCallExpr.getNameAsString())) {
      return false;
    }

    // fix the child reference
    Expression child = methodCallExpr.getScope().orElseThrow();
    Optional<MethodCallExpr> childGetCanonicalPathCall =
        expect(child).toBeMethodCallExpression().withName("getCanonicalPath").result();

    if (childGetCanonicalPathCall.isEmpty()) {
      return false;
    }

    Expression childScope = childGetCanonicalPathCall.get().getScope().get();
    MethodCallExpr newChild = new MethodCallExpr(childScope, "getCanonicalFile().toPath");
    methodCallExpr.setScope(newChild);

    // fix the parent reference
    Expression parent = methodCallExpr.getArgument(0);
    Optional<MethodCallExpr> parentGetCanonicalPathCall =
        expect(parent).toBeMethodCallExpression().withName("getCanonicalPath").result();

    if (parentGetCanonicalPathCall.isEmpty()) {
      return false;
    }

    Expression parentScope = parentGetCanonicalPathCall.get().getScope().get();
    MethodCallExpr newParentArgument = new MethodCallExpr(parentScope, "getCanonicalFile().toPath");
    methodCallExpr.setArgument(0, newParentArgument);

    return true;
  }
}
