package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import java.util.List;
import java.util.Optional;

/** Filter table name parameters in SQL queries */
@Codemod(
    id = "pixee:java/sql-table-injection-filter",
    importance = Importance.HIGH,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW)
public final class SQLTableInjectionFilterCodemod extends JavaParserChanger {

  private Optional<CodemodChange> onNodeFound(final MethodCallExpr methodCallExpr) {
    if (SQLTableInjectionFilterTransform.findAndFix(methodCallExpr)) {
      return Optional.of(CodemodChange.from(methodCallExpr.getBegin().get().line));
    }
    return Optional.empty();
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<CodemodChange> changes =
        cu.findAll(MethodCallExpr.class).stream()
            .flatMap(mce -> onNodeFound(mce).stream())
            .toList();
    return CodemodFileScanningResult.withOnlyChanges(changes);
  }
}
