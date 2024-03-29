package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.ast.ASTTransforms;
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
    var changes =
        cu.findAll(MethodCallExpr.class).stream()
            .flatMap(mce -> onNodeFound(mce).stream())
            .collect(Collectors.toList());
    // Cleanup, removes empty string concatenations and unused variables
    ASTTransforms.removeEmptyStringConcatenation(cu);
    // TODO hits a bug with javaparser, where adding nodes won't result in the correct children
    // order. This causes the following to remove actually used variables
    // ASTTransforms.removeUnusedLocalVariables(compilationUnit);
    return changes;
  }
}
