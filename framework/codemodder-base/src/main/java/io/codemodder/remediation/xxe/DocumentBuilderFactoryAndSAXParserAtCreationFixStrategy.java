package io.codemodder.remediation.xxe;

import static io.codemodder.javaparser.ASTExpectations.expect;
import static io.codemodder.remediation.xxe.XMLFixBuilder.addFeatureDisablingStatements;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import io.codemodder.remediation.MatchAndFixStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.List;
import java.util.Optional;

/**
 * Fix strategy for XXE vulnerabilities anchored to the parser's newInstance() calls. Finds the
 * parser's declaration and add statements disabling external entities and features.
 */
final class DocumentBuilderFactoryAndSAXParserAtCreationFixStrategy extends MatchAndFixStrategy {

  @Override
  public SuccessOrReason fix(CompilationUnit cu, Node node) {
    var maybeCall =
        Optional.of(node).map(m -> m instanceof MethodCallExpr ? (MethodCallExpr) node : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call");
    }
    MethodCallExpr newFactoryInstanceCall = maybeCall.get();
    Optional<VariableDeclarator> newFactoryVariableRef =
        expect(newFactoryInstanceCall).toBeMethodCallExpression().initializingVariable().result();
    VariableDeclarator newFactoryVariable = newFactoryVariableRef.get();
    Optional<Statement> variableDeclarationStmtRef =
        newFactoryVariable.findAncestor(Statement.class);

    if (variableDeclarationStmtRef.isEmpty()) {
      SuccessOrReason.reason("Not assigned as part of statement");
    }

    Statement statement = variableDeclarationStmtRef.get();
    return addFeatureDisablingStatements(
        newFactoryVariable.getNameAsExpression(), statement, false);
  }

  /**
   * Matches [DocumentBuilderFactory, SAXParserFactory] y = (x.)newInstance(), where the node is the
   * newInstance call.
   *
   * @param node
   * @return
   */
  public boolean match(final Node node) {
    return ASTs.isInitializedToType(
            node, "newInstance", List.of("DocumentBuilderFactory", "SAXParserFactory"))
        .isPresent();
  }
}
