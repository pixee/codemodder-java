package io.codemodder.remediation.xxe;

import static io.codemodder.remediation.RemediationMessages.multipleCallsFound;
import static io.codemodder.remediation.RemediationMessages.noCallsAtThatLocation;

import com.github.javaparser.Position;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Fix XXEs that are reported at the DocumentBuilder.parse() call locations. */
final class DocumentBuilderFactoryAtParseFixer implements XXEFixer {

  @Override
  public XXEFixAttempt tryFix(final int line, final Integer column, CompilationUnit cu) {
    List<MethodCallExpr> candidateMethods =
        cu.findAll(MethodCallExpr.class).stream()
            .filter(m -> "parse".equals(m.getNameAsString()))
            .filter(m -> m.getScope().isPresent())
            .filter(m -> m.getScope().get().isNameExpr())
            .filter(m -> m.getRange().isPresent() && m.getRange().get().begin.line == line)
            .toList();

    if (candidateMethods.size() > 1 && column != null) {
      candidateMethods =
          candidateMethods.stream()
              .filter(m -> m.getRange().get().contains(new Position(line, column)))
              .toList();
    }

    if (candidateMethods.isEmpty()) {
      return new XXEFixAttempt(false, false, noCallsAtThatLocation);
    } else if (candidateMethods.size() > 1) {
      return new XXEFixAttempt(false, false, multipleCallsFound);
    }

    // find the variable that we're calling parse() on
    MethodCallExpr parseCall = candidateMethods.get(0);

    // we know from the filter that it's scope is a name expression
    NameExpr documentBuilder = parseCall.getScope().get().asNameExpr();

    // so, we need to see if we can see the DocumentBuilderFactory from whence this came
    Optional<MethodDeclaration> methodBody = ASTs.findMethodBodyFrom(documentBuilder);
    if (methodBody.isEmpty()) {
      return new XXEFixAttempt(false, false, "No method body found for the call");
    }

    Optional<Node> documentBuilderAssignment =
        ASTs.findNonCallableSimpleNameSource(documentBuilder.getName());
    if (documentBuilderAssignment.isEmpty()) {
      return new XXEFixAttempt(false, false, "No assignment found for the DocumentBuilder");
    }
    Node parserAssignmentNode = documentBuilderAssignment.get();
    if (!(parserAssignmentNode instanceof NodeWithType<?, ?>)) {
      return new XXEFixAttempt(false, false, "Unknown DocumentBuilder assignment");
    }
    String parserType = ((NodeWithType<?, ?>) parserAssignmentNode).getTypeAsString();
    if (!Set.of("DocumentBuilder", "javax.xml.parsers.DocumentBuilder").contains(parserType)) {
      return new XXEFixAttempt(false, false, "Parsing method is not a DocumentBuilder");
    }

    if (parserAssignmentNode instanceof VariableDeclarator dbVar) {
      Optional<Expression> initializer = dbVar.getInitializer();
      if (initializer.isEmpty()) {
        return new XXEFixAttempt(
            false, false, "DocumentBuilder was not initialized in an expected way");
      } else if (!(initializer.get() instanceof MethodCallExpr)) {
        return new XXEFixAttempt(
            false, false, "DocumentBuilder was not initialized with a factory call");
      }
      MethodCallExpr potentialFactoryCall = (MethodCallExpr) initializer.get();
      if (!"newDocumentBuilder".equals(potentialFactoryCall.getNameAsString())) {
        return new XXEFixAttempt(
            false, false, "DocumentBuilder was initialized with newDocumentBuilder");
      }
      if (potentialFactoryCall.getScope().isEmpty()) {
        return new XXEFixAttempt(
            true, false, "DocumentBuilder was initialized with a factory call without a scope");
      }
      if (!(potentialFactoryCall.getScope().get() instanceof NameExpr)) {
        return new XXEFixAttempt(
            false,
            false,
            "DocumentBuilder was initialized with a factory call with a non-name scope");
      }
      NameExpr factoryNameExpr = (NameExpr) potentialFactoryCall.getScope().get();
      Optional<Statement> newDocumentBuilderStatement = ASTs.findParentStatementFrom(dbVar);
      if (newDocumentBuilderStatement.isEmpty()) {
        return new XXEFixAttempt(
            false,
            false,
            "DocumentBuilder was initialized with a factory call without a statement");
      }
      return XMLFeatures.addFeatureDisablingStatements(
          cu, factoryNameExpr, newDocumentBuilderStatement.get(), true);
    } else if (parserAssignmentNode instanceof Parameter) {
      return new XXEFixAttempt(true, false, "DocumentBuilder came from outside the method scope");
    }

    return new XXEFixAttempt(true, false, "DocumentBuilder was not initialized in an expected way");
  }
}
