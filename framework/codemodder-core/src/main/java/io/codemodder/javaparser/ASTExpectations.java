package io.codemodder.javaparser;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import java.util.Optional;

/**
 * A utility for validating the shape of ASTs and filtering them to retrieve. It only exposes one
 * endpoint, {@link #expect(Node)}, which allows folks to filter and telescope a further down an AST
 * subgraph.
 *
 * <p>The notable weakness of this API is use cases where you need to retrieve multiple parts of the
 * AST, but we have not run into that much as of yet.
 */
@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public final class ASTExpectations {

  private ASTExpectations() {}

  /** A producer of an AST subgraph. */
  interface ASTExpectationProducer<T extends Node> {
    Optional<T> result();
  }

  /** The starting point for validating the shape of an AST and returning a subgraph. */
  public static NodeExpectation expect(final Node node) {
    return new NodeExpectation(node);
  }

  /** A type for querying and filtering generic AST nodes. */
  public static class NodeExpectation implements ASTExpectationProducer<Node> {

    private Optional<Node> nodeRef;

    public NodeExpectation(final Node node) {
      this.nodeRef = Optional.of(node);
    }

    /** Return an expectation that asserts the parent node is a {@link BlockStmt}. */
    public NodeExpectation withBlockParent() {
      if (nodeRef.isEmpty()) {
        return this;
      }
      Optional<Node> parentRef = nodeRef.get().getParentNode();
      if (parentRef.isEmpty()) {
        nodeRef = Optional.empty();
        return this;
      }
      Node parent = parentRef.get();
      if (!(parent instanceof BlockStmt)) {
        nodeRef = Optional.empty();
      }
      return this;
    }

    /**
     * Return an expectation that asserts the node is an {@link ExpressionStmt} with a {@link
     * VariableDeclarationExpr} for its expression.
     */
    public VariableDeclarationExprExpectation toBeVariableDeclarationStatement() {
      if (nodeRef.isEmpty()) {
        return new VariableDeclarationExprExpectation(null);
      }
      Node node = nodeRef.get();
      if (!(node instanceof ExpressionStmt)) {
        nodeRef = Optional.empty();
        return new VariableDeclarationExprExpectation(null);
      }

      ExpressionStmt stmt = (ExpressionStmt) node;
      if (!(stmt.getExpression() instanceof VariableDeclarationExpr)) {
        nodeRef = Optional.empty();
        return new VariableDeclarationExprExpectation(null);
      }

      VariableDeclarationExpr variableDeclarator = ((VariableDeclarationExpr) stmt.getExpression());
      return new VariableDeclarationExprExpectation(variableDeclarator);
    }

    @Override
    public Optional<Node> result() {
      return nodeRef;
    }
  }

  /** A type for querying and filtering variable declaration(s). */
  public static class VariableDeclarationExprExpectation
      implements ASTExpectationProducer<VariableDeclarationExpr> {
    private Optional<VariableDeclarationExpr> varDefExpr;

    public VariableDeclarationExprExpectation(final VariableDeclarationExpr expr) {
      this.varDefExpr = Optional.ofNullable(expr);
    }

    /** Return an expectation that asserts the variable declaration has only a single variable. */
    public SingleVariableDeclaratorExpectation toBeSingleVariableDefinition() {
      if (varDefExpr.isEmpty()) {
        return new SingleVariableDeclaratorExpectation(null);
      }
      if (varDefExpr.get().getVariables().size() != 1) {
        varDefExpr = Optional.empty();
        return new SingleVariableDeclaratorExpectation(null);
      }
      return new SingleVariableDeclaratorExpectation(varDefExpr.get().getVariables().get(0));
    }

    @Override
    public Optional<VariableDeclarationExpr> result() {
      return varDefExpr;
    }
  }

  /** A type for querying and filtering a single variable declaration. */
  public static class SingleVariableDeclaratorExpectation {
    private Optional<VariableDeclarator> varRef;

    public SingleVariableDeclaratorExpectation(final VariableDeclarator variableDeclarator) {
      this.varRef = Optional.ofNullable(variableDeclarator);
    }

    /** Return an expectation that asserts the variable declaration has only a single variable. */
    public SingleVariableDeclaratorExpectation withDirectReferenceCount(final int count) {
      if (varRef.isEmpty()) {
        return this;
      }
      if (!Filters.isVariableReferencedExactly(varRef.get(), count)) {
        varRef = Optional.empty();
      }
      return this;
    }

    public MethodCallExpectation toBeInitializedByMethodCall() {
      if (varRef.isEmpty()) {
        return new MethodCallExpectation(null);
      }
      if (varRef.get().getInitializer().isEmpty()) {
        varRef = Optional.empty();
        return new MethodCallExpectation(null);
      }
      Expression initializer = varRef.get().getInitializer().get();
      if (!MethodCallExpr.class.equals(initializer.getClass())) {
        varRef = Optional.empty();
      }
      return new MethodCallExpectation((MethodCallExpr) initializer);
    }

    public Optional<VariableDeclarator> result() {
      return varRef;
    }
  }

  /** A type for querying and filtering method call expressions. */
  public static class MethodCallExpectation {
    private Optional<MethodCallExpr> methodCallExpr;

    public MethodCallExpectation(final MethodCallExpr methodCallExpr) {
      this.methodCallExpr = Optional.ofNullable(methodCallExpr);
    }

    public Optional<MethodCallExpr> result() {
      return methodCallExpr;
    }
  }
}
