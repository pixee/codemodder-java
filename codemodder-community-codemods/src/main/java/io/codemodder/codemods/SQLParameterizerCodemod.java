package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.Optional;

@Codemod(
    id = "pixee:java/sql-parameterizer",
    author = "andre.silva@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SQLParameterizerCodemod implements JavaParserChanger {

  private Optional<Weave> onNodeFound(
      final CodemodInvocationContext context,
      final MethodCallExpr methodCallExpr,
      final CompilationUnit cu) {
    if (new SQLParameterizer().checkAndFix(methodCallExpr)) {
      return Optional.of(Weave.from(methodCallExpr.getBegin().get().line, context.codemodId()));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public void visit(final CodemodInvocationContext context, final CompilationUnit cu) {
    cu.findAll(MethodCallExpr.class).stream()
        .flatMap(mce -> onNodeFound(context, mce, cu).stream())
        .forEach(w -> context.changeRecorder().addWeave(w));
  }
}
