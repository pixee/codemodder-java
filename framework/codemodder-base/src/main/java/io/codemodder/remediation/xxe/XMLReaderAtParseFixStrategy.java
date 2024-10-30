package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithType;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import io.codemodder.remediation.MatchAndFixStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;
import java.util.Set;

/**
 * Fix strategy for XXE vulnerabilities anchored to the XMLReader parse() calls. Finds the parser's
 * declaration and add statements disabling external entities and features.
 */
final class XMLReaderAtParseFixStrategy extends MatchAndFixStrategy {

  @Override
  public SuccessOrReason fix(CompilationUnit cu, Node node) {
    var maybeCall =
        Optional.of(node).map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call.");
    }
    MethodCallExpr parseCall = maybeCall.get();

    Optional<Expression> parserRef = parseCall.getScope();
    if (parserRef.isEmpty()) {
      return SuccessOrReason.reason("No scope found for parse() call");
    } else if (!parserRef.get().isNameExpr()) {
      return SuccessOrReason.reason("Scope is not a name expression");
    }
    NameExpr parser = parserRef.get().asNameExpr();

    Optional<Statement> parseStatement = parseCall.findAncestor(Statement.class);
    if (parseStatement.isEmpty()) {
      return SuccessOrReason.reason("No statement found for parse() call");
    }
    return XMLFixBuilder.addFeatureDisablingStatements(
        parser.asNameExpr(), parseStatement.get(), true);
  }

  /**
   * Matches against XMLReader.parse calls
   *
   * @param node
   * @return
   */
  public boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
        .filter(m -> "parse".equals(m.getNameAsString()))
        .filter(m -> m.getScope().isPresent())
        .filter(m -> m.getScope().get().isNameExpr())
        .filter(
            m -> {
              Optional<Node> sourceRef =
                  ASTs.findNonCallableSimpleNameSource(m.getScope().get().asNameExpr().getName());
              if (sourceRef.isEmpty()) {
                return false;
              }
              Node source = sourceRef.get();
              if (source instanceof NodeWithType<?, ?>) {
                return Set.of("XMLReader", "org.xml.sax.XMLReader")
                    .contains(((NodeWithType<?, ?>) source).getTypeAsString());
              }
              return false;
            })
        .isPresent();
  }
}
