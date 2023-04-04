package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.Codemod;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.JavaParserChanger;
import io.codemodder.ReviewGuidance;
import io.codemodder.Weave;
import io.openpixee.jdbcparameterizer.SQLParameterizer;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Codemod(
    id = "pixee:java/sql-parameterizer",
    author = "andre.silva@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SQLParameterizerCodemod implements JavaParserChanger {

  private List<Weave> onNodeFound(
      CodemodInvocationContext context, MethodCallExpr methodCallExpr, CompilationUnit cu) {
    var fixer = new SQLParameterizer(cu);
    var maybeChanges = fixer.parameterizeStatement(methodCallExpr, methodCallExpr.getArgument(0));
    if (maybeChanges.isLeft()) {
      return maybeChanges.getLeft().stream()
          .map(c -> Weave.from(c.getLine(), context.codemodId()))
          .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public void visit(final CodemodInvocationContext context, final CompilationUnit cu) {
    cu.findAll(MethodCallExpr.class).stream()
        .filter(mce -> SQLParameterizer.isParameterizationCandidate(mce))
        .flatMap(mce -> onNodeFound(context, mce, cu).stream())
        .forEach(w -> context.changeRecorder().addWeave(w));
  }
}
