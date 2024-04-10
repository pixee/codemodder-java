package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.codetf.FixedFinding;
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
    if (SQLParameterizerWithCleanup.checkAndFix(methodCallExpr)) {
      return Optional.of(CodemodChange.from(methodCallExpr.getBegin().get().line));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<CodemodChange> changes =
        cu.findAll(MethodCallExpr.class).stream()
            .flatMap(mce -> onNodeFound(mce).stream())
            .collect(Collectors.toList());
    return CodemodFileScanningResult.withOnlyChanges(changes);
  }

  @Override
  public Optional<FixedFinding> buildFixedFinding(final String id) {
    return Optional.empty();
  }
}
