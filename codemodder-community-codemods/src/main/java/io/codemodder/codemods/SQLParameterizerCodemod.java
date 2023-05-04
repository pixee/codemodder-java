package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import io.openpixee.jdbcparameterizer.SQLParameterizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Codemod(
    id = "pixee:java/sql-parameterizer",
    author = "andre.silva@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SQLParameterizerCodemod implements JavaParserChanger {

  private List<CodemodChange> onNodeFound(MethodCallExpr methodCallExpr, CompilationUnit cu) {
    var fixer = new SQLParameterizer(cu);
    var maybeChanges = fixer.parameterizeStatement(methodCallExpr, methodCallExpr.getArgument(0));
    if (maybeChanges.isLeft()) {
      return maybeChanges.getLeft().stream()
          .map(c -> CodemodChange.from(c.getLine()))
          .collect(Collectors.toList());
    } else {
      return Collections.emptyList();
    }
  }

  @Override
  public List<CodemodChange> visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    final List<CodemodChange> changes = new ArrayList<>();
    cu.findAll(MethodCallExpr.class).stream()
        .filter(SQLParameterizer::isParameterizationCandidate)
        .flatMap(mce -> onNodeFound(mce, cu).stream())
        .forEach(changes::add);
    return changes;
  }
}
