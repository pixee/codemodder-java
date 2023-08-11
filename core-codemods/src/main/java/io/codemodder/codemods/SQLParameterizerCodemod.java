package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Parameterizes SQL statements in the java JDBC api. */
@Codemod(id = "pixee:java/sql-parameterizer", reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SQLParameterizerCodemod extends JavaParserChanger {

  private Optional<CodemodChange> onNodeFound(
      final CodemodInvocationContext context,
      final MethodCallExpr methodCallExpr,
      final CompilationUnit cu) {
    if (new SQLParameterizer(methodCallExpr).checkAndFix()) {
      return Optional.of(CodemodChange.from(methodCallExpr.getBegin().get().line));
    } else {
      return Optional.empty();
    }
  }

  public List<CodemodChange> visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    return cu.findAll(MethodCallExpr.class).stream()
        .flatMap(mce -> onNodeFound(context, mce, cu).stream())
        .collect(Collectors.toList());
  }
}
