package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
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
 * Fix strategy for XXE vulnerabilities anchored to the parser's parse() calls. Finds the parser's
 * declaration and add statements disabling external entities and features.
 */
final class DocumentBuilderFactoryAtParseFixStrategy extends MatchAndFixStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {

    var maybeCall =
        Optional.of(node).map(m -> m instanceof MethodCallExpr ? (MethodCallExpr) node : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call");
    }
    // find the variable that we're calling parse() on
    MethodCallExpr parseCall = maybeCall.get();

    // we know from the filter that it's scope is a name expression
    NameExpr documentBuilder = parseCall.getScope().get().asNameExpr();

    // so, we need to see if we can see the DocumentBuilderFactory from whence this came
    Optional<MethodDeclaration> methodBody = ASTs.findMethodBodyFrom(documentBuilder);
    if (methodBody.isEmpty()) {
      return SuccessOrReason.reason("No method body found for the call");
    }

    Optional<Node> documentBuilderAssignment =
        ASTs.findNonCallableSimpleNameSource(documentBuilder.getName());
    if (documentBuilderAssignment.isEmpty()) {
      return SuccessOrReason.reason("No assignment found for the DocumentBuilder");
    }
    Node parserAssignmentNode = documentBuilderAssignment.get();
    if (!(parserAssignmentNode instanceof NodeWithType<?, ?>)) {
      return SuccessOrReason.reason("Unknown DocumentBuilder assignment");
    }
    String parserType = ((NodeWithType<?, ?>) parserAssignmentNode).getTypeAsString();
    if (!Set.of("DocumentBuilder", "javax.xml.parsers.DocumentBuilder").contains(parserType)) {
      return SuccessOrReason.reason("Parsing method is not a DocumentBuilder");
    }

    if (parserAssignmentNode instanceof VariableDeclarator dbVar) {
      Optional<Expression> initializer = dbVar.getInitializer();
      if (initializer.isEmpty()) {
        return SuccessOrReason.reason("DocumentBuilder was not initialized in an expected way");
      } else if (!(initializer.get() instanceof MethodCallExpr)) {
        return SuccessOrReason.reason("DocumentBuilder was not initialized with a factory call");
      }
      MethodCallExpr potentialFactoryCall = (MethodCallExpr) initializer.get();
      if (!"newDocumentBuilder".equals(potentialFactoryCall.getNameAsString())) {
        return SuccessOrReason.reason("DocumentBuilder was initialized with newDocumentBuilder");
      }
      if (potentialFactoryCall.getScope().isEmpty()) {
        return SuccessOrReason.reason(
            "DocumentBuilder was initialized with a factory call without a scope");
      }
      if (!(potentialFactoryCall.getScope().get() instanceof NameExpr)) {
        return SuccessOrReason.reason(
            "DocumentBuilder was initialized with a factory call with a non-name scope");
      }
      NameExpr factoryNameExpr = (NameExpr) potentialFactoryCall.getScope().get();
      Optional<Statement> newDocumentBuilderStatement = ASTs.findParentStatementFrom(dbVar);
      if (newDocumentBuilderStatement.isEmpty()) {
        return SuccessOrReason.reason(
            "DocumentBuilder was initialized with a factory call without a statement");
      }
      return XMLFixBuilder.addFeatureDisablingStatements(
          factoryNameExpr, newDocumentBuilderStatement.get(), true);
    } else if (parserAssignmentNode instanceof Parameter) {
      return SuccessOrReason.reason("DocumentBuilder came from outside the method scope");
    }

    return SuccessOrReason.reason("DocumentBuilder was not initialized in an expected way");
  }

  /**
   * Matches against x.parse() calls
   *
   * @param node
   * @return
   */
  public boolean match(final Node node) {
    return Optional.of(node)
        .map(n -> n instanceof MethodCallExpr ? (MethodCallExpr) n : null)
        .filter(m -> "parse".equals(m.getNameAsString()))
        .filter(m -> m.getScope().filter(Expression::isNameExpr).isPresent())
        .isPresent();
  }
}
