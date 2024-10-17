package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.remediation.resourceleak.ResourceLeakFixer;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** A codemod that wraps AutoCloseable objects whenever possible. */
@Codemod(
    id = "pixee:java/resource-leak",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.LOW)
public final class ResourceLeakCodemod extends JavaParserChanger {

  private Optional<CodemodChange> onNodeFound(final Expression expr) {
    int originalLine = expr.getBegin().get().line;
    if (ResourceLeakFixer.checkAndFix(expr).isPresent()) {
      return Optional.of(CodemodChange.from(originalLine));
    } else {
      return Optional.empty();
    }
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<CodemodChange> changes =
        cu.findAll(Expression.class).stream()
            .flatMap(expr -> onNodeFound(expr).stream())
            .collect(Collectors.toList());
    return CodemodFileScanningResult.withOnlyChanges(changes);
  }
}
