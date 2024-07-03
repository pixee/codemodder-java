package io.codemodder.remediation.xss;

import static io.codemodder.javaparser.JavaParserTransformer.wrap;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.RemediationMessages;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/**
 * Fixes a method invocation that prints {@link String} expression.
 *
 * <pre>{@code
 * out.println(foo); // should become return out.println(Encode.forHtml(foo));
 * }</pre>
 */
final class PrintingMethodFixer implements XSSCodeShapeFixer {

  private static final Set<String> writingMethodNames = Set.of("print", "println", "write");

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

    List<MethodCallExpr> writingMethodCalls =
        cu.findAll(MethodCallExpr.class).stream()
            .filter(mce -> writingMethodNames.contains(mce.getNameAsString()))
            .filter(mce -> mce.getArguments().size() == 1)
            .filter(mce -> mce.getRange().isPresent())
            .filter(mce -> mce.getRange().get().begin.line == line)
            .filter(mce -> column == null || mce.getRange().get().begin.column == column)
            .filter(
                mce -> {
                  Expression arg = mce.getArgument(0);
                  if (arg instanceof NameExpr nameExpr) {
                    Optional<Node> source =
                        ASTs.findNonCallableSimpleNameSource(nameExpr.getName());
                    if (source.isPresent()
                        && source.get() instanceof NodeWithType<?, ?> typedNode) {
                      String typeAsString = typedNode.getTypeAsString();
                      return "String".equals(typeAsString)
                          || "java.lang.String".equals(typeAsString);
                    }
                  }
                  return false;
                })
            .toList();

    if (writingMethodCalls.isEmpty()) {
      return new XSSCodeShapeFixResult(false, false, null, line);
    } else if (writingMethodCalls.size() > 1) {
      return new XSSCodeShapeFixResult(true, false, RemediationMessages.multipleCallsFound, line);
    }

    MethodCallExpr call = writingMethodCalls.get(0);
    wrap(call.getArgument(0)).withStaticMethod("org.owasp.encoder.Encoder", "forHtml", false);
    return new XSSCodeShapeFixResult(true, true, null, line);
  }
}
