package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
import io.codemodder.ast.ASTs;
import java.util.List;
import java.util.Optional;

/** Holds shared APIs for working with shared XML feature setting. */
final class XMLFeatures {

  private XMLFeatures() {}

  /** Creates a call that disables parameter entities. */
  static MethodCallExpr createParameterEntityDisabledCall(final NameExpr nameExpr) {
    return new MethodCallExpr(
        nameExpr,
        "setFeature",
        NodeList.nodeList(
            new StringLiteralExpr("http://xml.org/sax/features/external-parameter-entities"),
            new BooleanLiteralExpr(false)));
  }

  /** Creates a call that disables general entities. */
  static MethodCallExpr createGeneralEntityDisablingCall(final NameExpr nameExpr) {
    return new MethodCallExpr(
        nameExpr,
        "setFeature",
        NodeList.nodeList(
            new StringLiteralExpr("http://xml.org/sax/features/external-general-entities"),
            new BooleanLiteralExpr(false)));
  }

  /**
   * Creates a call for {@link javax.xml.stream.XMLInputFactory#setProperty(java.lang.String,
   * java.lang.Object)} for disabling.
   */
  static XXEFixAttempt addXMLInputFactoryDisablingStatement(
      final NameExpr variable, final Statement statementToInjectAround, final boolean before) {
    MethodCallExpr call =
        new MethodCallExpr(
            variable,
            "setProperty",
            NodeList.nodeList(
                new StringLiteralExpr("javax.xml.stream.isSupportingExternalEntities"),
                new BooleanLiteralExpr(false)));

    Optional<BlockStmt> block = ASTs.findBlockStatementFrom(statementToInjectAround);
    if (block.isEmpty()) {
      return new XXEFixAttempt(true, false, "No block statement found for newFactory() call");
    }

    BlockStmt blockStmt = block.get();
    NodeList<Statement> existingStatements = blockStmt.getStatements();
    int index = existingStatements.indexOf(statementToInjectAround);
    if (!before) {
      index++;
    }

    Statement fixStatement = new ExpressionStmt(call);
    existingStatements.add(index, fixStatement);
    return new XXEFixAttempt(true, true, null);
  }

  static XXEFixAttempt addFeatureDisablingStatements(
      final NameExpr variable, final Statement statementToInjectAround, final boolean before) {
    Optional<BlockStmt> block = ASTs.findBlockStatementFrom(statementToInjectAround);
    if (block.isEmpty()) {
      return new XXEFixAttempt(true, false, "No block statement found for newFactory() call");
    }

    BlockStmt blockStmt = block.get();
    MethodCallExpr setFeatureGeneralEntities = createGeneralEntityDisablingCall(variable);
    MethodCallExpr setFeatureParameterEntities = createParameterEntityDisabledCall(variable);
    List<Statement> fixStatements =
        List.of(
            new ExpressionStmt(setFeatureGeneralEntities),
            new ExpressionStmt(setFeatureParameterEntities));

    NodeList<Statement> existingStatements = blockStmt.getStatements();
    int index = existingStatements.indexOf(statementToInjectAround);
    if (!before) {
      index++;
    }
    existingStatements.addAll(index, fixStatements);

    // search for any dbf.setExpandEntityReferences(true) calls and remove them
    blockStmt.findAll(MethodCallExpr.class).stream()
        .filter(
            m ->
                "setExpandEntityReferences".equals(m.getNameAsString())
                    && m.getScope().isPresent()
                    && m.getScope().get().equals(variable)
                    && m.getArguments().size() == 1
                    && m.getArguments().get(0).isBooleanLiteralExpr()
                    && m.getArguments().get(0).asBooleanLiteralExpr().getValue())
        .map(Node::getParentNode)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .forEach(Node::remove);

    return new XXEFixAttempt(true, true, null);
  }
}
