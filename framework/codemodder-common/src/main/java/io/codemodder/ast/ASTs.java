package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.type.TypeParameter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Utility methods that may be useful for many visitors. */
public final class ASTs {

  /**
   * Searches up the AST to find the method body from the given {@link Node}. There could be orphan
   * statements like variable declarations outside a constructor.
   */
  public static Optional<MethodDeclaration> findMethodBodyFrom(Node node) {
    while (node.getParentNode().isPresent()
        && !(node.getParentNode().get() instanceof MethodDeclaration)) {
      node = node.getParentNode().get();
    }
    final var methodDeclarationOrNullRef = node.getParentNode();
    return methodDeclarationOrNullRef.map(value -> (MethodDeclaration) value);
  }

  /**
   * Searches up the AST to find the {@link BlockStmt} given the {@link Node}. Eventually these
   * other methods should be refactored to use {@link Optional} patterns.
   */
  public static Optional<BlockStmt> findBlockStatementFrom(Node node) {
    while (node.getParentNode().isPresent() && !(node.getParentNode().get() instanceof BlockStmt)) {
      node = node.getParentNode().get();
    }
    if (node.getParentNode().isPresent() && node.getParentNode().get() instanceof BlockStmt) {
      return Optional.of((BlockStmt) node.getParentNode().get());
    }
    return Optional.empty();
  }

  /** Searches up the AST to find the {@link Statement} given the {@link Node}. */
  public static Optional<Statement> findParentStatementFrom(Node node) {
    while (node.getParentNode().isPresent() && !(node.getParentNode().get() instanceof Statement)) {
      node = node.getParentNode().get();
    }
    if (node.getParentNode().isPresent() && node.getParentNode().get() instanceof Statement) {
      return Optional.of((Statement) node.getParentNode().get());
    }
    return Optional.empty();
  }

  /**
   * Searches up the AST to find the {@link ClassOrInterfaceDeclaration} given {@link Node}. There
   * could be orphan statements I guess in stray Java files, so return null if we ever run into
   * that? Not sure how expected that will be, so not sure if I should make it an exception-based
   * pattern.
   */
  public static ClassOrInterfaceDeclaration findTypeFrom(Node node) {
    while (node.getParentNode().isPresent()
        && !(node.getParentNode().get() instanceof ClassOrInterfaceDeclaration)) {
      node = node.getParentNode().get();
    }
    final var type = node.getParentNode();
    return (ClassOrInterfaceDeclaration) type.orElse(null);
  }

  /**
   * Given a {@link LocalVariableDeclaration} verifies if it is final or never assigned. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html">Java Language
   * Specification - Section 4.12.14</a> for the definitions of final variables.
   */
  public static boolean isFinalOrNeverAssigned(final LocalVariableDeclaration lvd) {
    return isFinalOrNeverAssigned(lvd.getVariableDeclarator(), lvd.getScope());
  }

  /**
   * Given a {@link SimpleName} {@code name} and a {@link VariableDeclarationExpr} with a declarator
   * of {@code name}, verifies if {@code name} is final or never assigned. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html">Java Language
   * Specification - Section 4.12.14</a> for the definitions of final variables.
   */
  public static boolean isFinalOrNeverAssigned(
      final VariableDeclarator vd, final LocalVariableScope scope) {
    // Assumes vde contains a declarator with name
    final var vde = (VariableDeclarationExpr) vd.getParentNode().get();
    // has final modifier
    if (vde.isFinal()) return true;

    // Effectively Final: never operand of unary operator
    final Predicate<UnaryExpr> isOperand =
        ue ->
            ue.getExpression().isNameExpr()
                && ue.getExpression()
                    .asNameExpr()
                    .getName()
                    .asString()
                    .equals(vd.getName().asString());
    for (final var expr : scope.getExpressions()) {
      if (expr.findFirst(UnaryExpr.class, isOperand).isPresent()) return false;
    }
    for (final var stmt : scope.getStatements()) {
      if (stmt.findFirst(UnaryExpr.class, isOperand).isPresent()) return false;
    }

    final Predicate<AssignExpr> isLHS =
        ae ->
            ae.getTarget().isNameExpr()
                && ae.getTarget().asNameExpr().getNameAsString().equals(vd.getNameAsString());
    if (vd.getInitializer().isPresent()) {
      for (final var stmt : scope.getStatements())
        if (stmt.findFirst(AssignExpr.class, isLHS).isPresent()) return false;
      for (final var expr : scope.getExpressions())
        if (expr.findFirst(AssignExpr.class, isLHS).isPresent()) return false;
      return true;
    }
    // If not initialized, always definitively unassigned whenever lhs of assignment
    return false;
  }

  /** Finds the {@link ClassOrInterfaceDeclaration} that is referenced by a {@link ThisExpr}. */
  public static ClassOrInterfaceDeclaration findThisDeclaration(final ThisExpr thisExpr) {
    return NameResolver.findThisDeclaration(thisExpr);
  }
  /** Checks if a local variable is not initialized and is assigned at most once. */
  public static boolean isNotInitializedAndAssignedAtMostOnce(LocalVariableDeclaration lvd) {
    final Predicate<AssignExpr> isLHS =
        ae ->
            ae.getTarget().isNameExpr()
                && ae.getTarget()
                    .asNameExpr()
                    .getName()
                    .asString()
                    .equals(lvd.getVariableDeclarator().getName().asString());

    if (lvd.getVariableDeclarator().getInitializer().isEmpty()) {
      final var allAssignments =
          Stream.concat(
              lvd.getScope().getExpressions().stream()
                  .flatMap(e -> e.findAll(AssignExpr.class, isLHS).stream()),
              lvd.getScope().getStatements().stream()
                  .flatMap(s -> s.findAll(AssignExpr.class, isLHS).stream()));
      return allAssignments.count() == 1;
    }
    return false;
  }

  /**
   * Returns a {@link List} containing all the referenced of {@code localDeclaration} in its scope.
   */
  public static List<NameExpr> findAllReferences(LocalVariableDeclaration localDeclaration) {
    return localDeclaration.getScope().stream()
        .flatMap(
            n ->
                n
                    .findAll(
                        NameExpr.class,
                        ne -> ne.getNameAsString().equals(localDeclaration.getName()))
                    .stream())
        .collect(Collectors.toList());
  }

  /**
   * Returns an iterator for all the nodes in the AST that precedes {@code n} in the pre-order
   * ordering.
   */
  public static ReverseEvaluationOrder reversePreOrderIterator(final Node n) {
    if (n.getParentNode().isPresent()) {
      final int pos = n.getParentNode().get().getChildNodes().indexOf(n);
      return new ReverseEvaluationOrder(n, pos);
    } else {
      return new ReverseEvaluationOrder(n, 0);
    }
  }

  /**
   * A {@link Node} iterator iterating over all the nodes that precedes a given node in the
   * pre-order of its AST.
   */
  public static final class ReverseEvaluationOrder implements Iterator<Node> {

    private Node current;
    private int posFromParent;

    ReverseEvaluationOrder(final Node n, final int posFromParent) {
      this.current = n;
      this.posFromParent = posFromParent;
    }

    @Override
    public Node next() {
      final var parent = current.getParentNode().get();
      if (posFromParent == 0) {
        current = current.getParentNode().get();
        if (current.getParentNode().isPresent()) {
          posFromParent = current.getParentNode().get().getChildNodes().indexOf(current);
        } else {
          posFromParent = 0;
        }
      } else {
        current = parent.getChildNodes().get(--posFromParent);
      }
      return current;
    }

    @Override
    public boolean hasNext() {
      return current.getParentNode().isPresent();
    }
  }

  /**
   * Tries to find the declaration that originates a {@link SimpleName} use that is a Simple
   * Expression Name, Simple Type Name, or Type Parameter within the AST. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.5.6.1">Java Language
   * Specification - 6.5.6.1 Simple Expression Names </a> and <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.5.5.1">Java Language
   * Specification - 6.5.5.1 Simple Type Names </a>.
   *
   * @return a {@link Node} that contains a declaration of {@code name} if it exists within the
   *     file. Will be one of the following: {@link Parameter}, {@link VariableDeclarator}, {@link
   *     TypeParameter}, {@link RecordDeclaration}, {@link PatternExpr}, {@link
   *     ClassOrInterfaceDeclaration}.
   */
  public static Optional<Node> findNonCallableSimpleNameSource(final SimpleName name) {
    return findNonCallableSimpleNameSource(name, name.asString());
  }

  /**
   * Tries to find a declaration of {@code name} that is in scope at the given {@link Node} {@code
   * start} within the AST. It assumes {@code name } is either a Simple Expression name, Simple Type
   * Name or Type Parameter. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.5.6.1">Java Language
   * Specification - 6.5.6.1 Simple Expression Names </a> and <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.5.5.1">Java Language
   * Specification - 6.5.5.1 Simple Type Names </a>.
   *
   * @return a {@link Node} that contains a declaration of {@code name} if it exists within the
   *     file. Will be one of the following: {@link Parameter}, {@link VariableDeclarator}, {@link
   *     TypeParameter}, {@link RecordDeclaration}, {@link PatternExpr}, {@link
   *     ClassOrInterfaceDeclaration}.
   */
  public static Optional<Node> findNonCallableSimpleNameSource(
      final Node start, final String name) {
    return NameResolver.resolveSimpleName(start, name);
  }

  /**
   * Staring from the {@link Node} {@code start}, checks if there exists a local declaration whose
   * name is {@code name}.
   */
  public static Optional<LocalVariableDeclaration> findEarliestLocalDeclarationOf(
      final Node start, final String name) {
    return NameResolver.findLocalDeclarationOf(start, name);
  }
}
