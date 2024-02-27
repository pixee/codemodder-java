package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** Parameterizes SQL statements in the JDBC API. */
@Codemod(
    id = "pixee:java/sql-parameterizer",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class SQLParameterizerCodemod extends JavaParserChanger {

  private Optional<CodemodChange> onNodeFound(final MethodCallExpr methodCallExpr) {
    if (new SQLParameterizer(methodCallExpr).checkAndFix()) {
      return Optional.of(CodemodChange.from(methodCallExpr.getBegin().get().line));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public List<CodemodChange> visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    return cu.findAll(MethodCallExpr.class).stream()
        .flatMap(mce -> onNodeFound(mce).stream())
        .collect(Collectors.toList());
  }
}
