package io.codemodder.remediation.xss;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.RemediationMessages;
import java.util.List;
import java.util.function.Function;

/**
 * Fixes a return statement that returns a {@link String} expression.
 *
 * <pre>{@code
 * return foo; // should become return Encode.forHtml(foo)
 * }</pre>
 */
final class NakedVariableReturnFixer implements XSSCodeShapeFixer {

  @Override
  public <T> XSSCodeShapeFixResult fixCodeShape(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issues,
      final Function<T, String> getKey,
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {
    int line = getLine.apply(issues.get(0));
    Integer column = getColumn.apply(issues.get(0));

    List<ReturnStmt> matchingStatements =
        cu.findAll(ReturnStmt.class).stream()
            .filter(rs -> rs.getRange().isPresent())
            .filter(rs -> rs.getRange().get().begin.line == line)
            .filter(rs -> column == null || rs.getRange().get().begin.column == column)
            .filter(rs -> rs.getExpression().isPresent())
            .filter(rs -> rs.getExpression().get().isNameExpr())
            .toList();

    if (matchingStatements.isEmpty()) {
      return new XSSCodeShapeFixResult(false, false, null, line);
    } else if (matchingStatements.size() > 1) {
      return new XSSCodeShapeFixResult(true, false, RemediationMessages.multipleCallsFound, line);
    }

    ReturnStmt nakedReturn = matchingStatements.get(0);
    wrap(nakedReturn.getExpression().get())
        .withStaticMethod("org.owasp.encoder.Encode", "forHtml", false);

    return new XSSCodeShapeFixResult(true, true, null, line);
  }
}
