package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;

final class XMLInputFactoryAtNewInstanceFixStrategy
    extends io.codemodder.remediation.MatchAndFixStrategy {

  /**
   * Matches (XmlInputFactory | var xif) y = XMLInputFactory.newInstance(), where the node is the
   * newDocumentBuilder call.
   *
   * @param node the node to match
   * @return true if this is an assignment to an XMLInputFactory
   */
  @Override
  public boolean match(final Node node) {
    Optional<MethodCallExpr> method =
        ASTs.isInitializedToType(node, "newInstance", List.of("var", "XMLInputFactory"));
    if (method.isEmpty()) {
      return false;
    }
    MethodCallExpr newInstance = method.get();
    Optional<Expression> scope = newInstance.getScope();
    if (scope.isEmpty()) {
      return false;
    }
    return scope.get().toString().equals("XMLInputFactory");
  }

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {
    MethodCallExpr newInstance = (MethodCallExpr) node;
    Optional<VariableDeclarator> initExpr = ASTs.isInitExpr(newInstance);
    if (initExpr.isEmpty()) {
      return SuccessOrReason.reason("Not an initialization expression");
    }
    VariableDeclarator xmlInputFactoryVariable = initExpr.get();
    Optional<Statement> variableDeclarationStmtRef =
        xmlInputFactoryVariable.findAncestor(Statement.class);

    if (variableDeclarationStmtRef.isEmpty()) {
      return SuccessOrReason.reason("Not assigned as part of statement");
    }

    Statement stmt = variableDeclarationStmtRef.get();
    return XMLFixBuilder.addXMLInputFactoryDisablingStatement(
        xmlInputFactoryVariable.getNameAsExpression(), stmt, false);
  }
}
