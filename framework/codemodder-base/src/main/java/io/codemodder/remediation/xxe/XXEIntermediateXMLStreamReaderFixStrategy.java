package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.remediation.RemediationStrategy;
import io.codemodder.remediation.SuccessOrReason;
import java.util.Optional;

public class XXEIntermediateXMLStreamReaderFixStrategy implements RemediationStrategy {

  @Override
  public SuccessOrReason fix(final CompilationUnit cu, final Node node) {

    var maybeCall =
        Optional.of(node).map(m -> m instanceof MethodCallExpr ? (MethodCallExpr) node : null);
    if (maybeCall.isEmpty()) {
      return SuccessOrReason.reason("Not a method call");
    }
    // get the xmlstreamreader scope variable
    MethodCallExpr createXMLStreamReaderCall = maybeCall.get();
    Expression xmlStreamReaderScope = createXMLStreamReaderCall.getScope().get();

    // make sure it's a variable
    if (!xmlStreamReaderScope.isNameExpr()) {
      return SuccessOrReason.reason("Could not find the XMLStreamReader variable");
    }
    // get the variable
    NameExpr xmlStreamReaderVariable = xmlStreamReaderScope.asNameExpr();
    // get the JavaParser statement that contains the create call
    Optional<Statement> ancestorStatement = createXMLStreamReaderCall.findAncestor(Statement.class);

    if (ancestorStatement.isEmpty()) {
      return SuccessOrReason.reason(
          "Could not find the statement containing the " + "XMLStreamReader " + "creation");
    }
    Statement stmt = ancestorStatement.get();
    return XMLFixBuilder.addXMLInputFactoryDisablingStatement(xmlStreamReaderVariable, stmt, true);
  }
}
