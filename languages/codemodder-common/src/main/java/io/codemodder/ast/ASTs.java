package io.codemodder.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.TypeParameter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.javatuples.Triplet;

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

  /** Return a string description of the type and method that contain this {@link Node}. */
  public static String describeType(final Node node) {
    final var cu = node.findCompilationUnit().get();
    final var filePackage = cu.getPackageDeclaration().orElse(new PackageDeclaration());
    final var packageName = filePackage.getNameAsString();
    final var type = cu.getPrimaryTypeName().orElse("unknown_type");
    return packageName.isBlank() ? type : packageName + "." + type;
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
   * Given a {@link VariableDeclarationExpr} {@code vde} returns the scope of the variables declared
   * within. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.3">Java Language
   * Specification - Section 6.3 </a>} for how the scope of local declarations are defined.
   */
  public static LocalVariableScope findLocalVariableScope(final VariableDeclarator vd) {
    final var p = vd.getParentNode().get().getParentNode().get();
    // Statement of local variable declarations are always contained in blocks
    if (p instanceof ExpressionStmt)
      return LocalVariableScope.fromLocalDeclaration((ExpressionStmt) p, vd);
    if (p instanceof ForEachStmt) return LocalVariableScope.fromForEachDeclaration((ForEachStmt) p);
    // The scope of declarations in ForStmt init and TryStmt resources also covers the expressions
    // that trails them and their bodies.
    if (p instanceof TryStmt) return LocalVariableScope.fromTryResource((TryStmt) p, vd);
    if (p instanceof ForStmt) return LocalVariableScope.fromForDeclaration((ForStmt) p, vd);
    // Should not happen
    return null;
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
        return current;
      } else {
        current = parent.getChildNodes().get(--posFromParent);
        return current;
      }
    }

    @Override
    public boolean hasNext() {
      return current.getParentNode().isPresent();
    }
  }

  private static Optional<Node> isLocalNameSource(final Node n, final String name) {
    final Optional<Node> maybe =
        ASTPatterns.isExpressionStmtDeclarationOf(n, name).map(Triplet::getValue2);
    // Possible returns:
    return maybe
        .or(() -> ASTPatterns.isResourceOf(n, name).map(Triplet::getValue2))
        .or(() -> ASTPatterns.isForVariableDeclarationOf(n, name).map(Triplet::getValue2))
        .or(() -> ASTPatterns.isForEachVariableDeclarationOf(n, name).map(Triplet::getValue2))
        .or(() -> ASTPatterns.isLambdaExprParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTPatterns.isExceptionParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTPatterns.isMethodFormalParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTPatterns.isMethodTypeParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTPatterns.isConstructorFormalParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTPatterns.isConstructorTypeParameterOf(n, name).map(Pair::getValue1))
        .or(() -> ASTPatterns.isLocalTypeDeclarationOf(n, name).map(Pair::getValue1))
        .or(() -> ASTPatterns.isLocalRecordDeclarationOf(n, name).map(Pair::getValue1))
        .or(() -> ASTPatterns.isPatternExprDeclarationOf(n, name).map(t -> t));
  }

  private static Optional<Node> findLocalNameSource(Node current, final String name) {
    // Traverse the tree in reverse pre-order until it hits a declaration
    final var it = reversePreOrderIterator(current);
    while (!(current instanceof TypeDeclaration) && it.hasNext()) {
      current = it.next();
      final var maybeFound = isLocalNameSource(current, name);
      if (maybeFound.isPresent()) return maybeFound;
    }
    return Optional.empty();
  }

  private static Optional<Node> findClassLevelNameSource(
      final ClassOrInterfaceDeclaration classDeclaration, final String name) {
    var maybeClassMemberDeclaration =
        Optional.of(classDeclaration)
            .filter(cd -> cd.getNameAsString().equals(name))
            .map(c -> (Node) c)
            .or(
                () ->
                    classDeclaration.getFields().stream()
                        .flatMap(field -> ASTPatterns.isFieldDeclarationOf(field, name).stream())
                        .findAny()
                        .map(p -> p.getValue1()))
            .or(
                () ->
                    classDeclaration.getMembers().stream()
                        .flatMap(bodyDecl -> ASTPatterns.isNamedMemberOf(bodyDecl, name).stream())
                        .findAny()
                        .map(nwn -> (Node) nwn))
            .or(
                () ->
                    ASTPatterns.isClassTypeParameterOf(classDeclaration, name)
                        .map(Pair::getValue0));
    if (maybeClassMemberDeclaration.isPresent()) {
      return maybeClassMemberDeclaration;
    }
    return Optional.empty();
  }

  /** Finds the {@link ClassOrInterfaceDeclaration} that is referenced by a {@link ThisExpr}. */
  public static ClassOrInterfaceDeclaration findThisDeclaration(final ThisExpr thisExpr) {
    Node current = thisExpr;
    while (current.hasParentNode() && !(current instanceof ClassOrInterfaceDeclaration)) {
      current = current.getParentNode().get();
    }
    return (ClassOrInterfaceDeclaration) current;
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
    // Callable names need more context like signatures to be found. Also, can be overloaded
    Node current = start;
    // Alternate its search from local (i.e. method level) to class level. It may happen because of
    // local type declarations.
    while (current.hasParentNode()) {
      current = current.getParentNode().get();
      // try locally first
      Optional<Node> maybeDeclaration = findLocalNameSource(current, name);
      if (maybeDeclaration.isPresent()) {
        return maybeDeclaration;
      }
      // No local declaration. Either hit root or a TypeDeclaration after its search
      // TypeDeclaration: ClassOrInterfaceDeclaration, EnumDeclaration, RecordDeclaration
      if (current instanceof ClassOrInterfaceDeclaration) {
        final var classDeclaration = (ClassOrInterfaceDeclaration) current;
        Optional<Node> maybeClassMember = findClassLevelNameSource(classDeclaration, name);
        if (maybeClassMember.isPresent()) {
          return maybeClassMember;
        }
      }
    }
    // reached CompilationUnit check for top level classes
    final var topLevelTypes = current.findCompilationUnit().get().getTypes();
    final var maybeDecl =
        topLevelTypes.stream().filter(t -> t.getNameAsString().equals(name)).findFirst();
    if (maybeDecl.isPresent()) return maybeDecl.map(n -> n);

    // it's either wildcard imported, inherited, or in the package namespace
    return Optional.empty();
  }

  /**
   * Staring from the {@link Node} {@code start}, checks if there exists a local declaration whose
   * name is {@code name}.
   */
  public static Optional<LocalVariableDeclaration> findEarliestLocalDeclarationOf(
      final Node start, final String name) {
    final var maybeSource =
        findNonCallableSimpleNameSource(start, name)
            .map(n -> n instanceof VariableDeclarator ? (VariableDeclarator) n : null);
    if (maybeSource.isPresent()) {
      final var vd = maybeSource.get();
      final Optional<LocalVariableDeclaration> maybeEVD =
          ASTPatterns.isVariableOfLocalDeclarationStmt(vd)
              .map(
                  t ->
                      new ExpressionStmtVariableDeclaration(
                          t.getValue0(), t.getValue1(), t.getValue2()));
      return maybeEVD
          .or(
              () ->
                  ASTPatterns.isResource(vd)
                      .map(p -> new TryResourceDeclaration(p.getValue0(), p.getValue1(), vd)))
          .or(
              () ->
                  ASTPatterns.isForInitVariable(vd)
                      .map(p -> new ForInitDeclaration(p.getValue0(), p.getValue1(), vd)))
          .or(
              () ->
                  ASTPatterns.isForEachVariable(vd)
                      .map(p -> new ForEachDeclaration(p.getValue0(), p.getValue1(), vd)));
    }

    return Optional.empty();
  }

  /**
   * Returns the unique path from {@code n} to the root of the tree. The path includes {@code n}
   * itself.
   */
  public static ArrayList<Node> pathToRoot(final Node n) {
    final ArrayList<Node> path = new ArrayList<>(List.of(n));
    var currentNode = n;
    while (currentNode.getParentNode().isPresent()) {
      path.add(currentNode.getParentNode().get());
      currentNode = currentNode.getParentNode().get();
    }
    return path;
  }

  /** Returns the lowest common ancestor of a pair of nodes. */
  public static Node lowestCommonAncestor(final Node n1, final Node n2) {
    final var n1Path = pathToRoot(n1);
    final var n2Path = pathToRoot(n2);
    var i1 = n1Path.size() - 1;
    var i2 = n2Path.size() - 1;
    while (n1Path.get(i1).equals(n2Path.get(i2)) && i1 >= 0 && i2 >= 0) {
      i1 -= 1;
      i2 -= 1;
    }
    return n1Path.get(i1);
  }

  /** Returns true if and only if {@code n1} &lt;= {@code n2} in post-order. */
  public static boolean postOrderLessThanOrEqual(final Node n1, final Node n2) {
    if (n1.equals(n2)) return true;
    final var n1Path = pathToRoot(n1);
    final var n2Path = pathToRoot(n2);
    var i1 = n1Path.size() - 1;
    var i2 = n2Path.size() - 1;
    while (i1 >= 0 && i2 >= 0 && n1Path.get(i1).equals(n2Path.get(i2))) {
      i1 -= 1;
      i2 -= 1;
    }
    // lowestCommonAncestor
    final var lca = n1Path.get(i1 + 1);
    System.out.println(lca);
    if (lca.equals(n2)) return true;
    if (lca.equals(n1)) return false;

    final var lcaDirectChild1 = n1Path.get(i1);
    final var lcaDirectChild2 = n2Path.get(i2);

    for (final Node n : lca.getChildNodes()) {
      if (n.equals(lcaDirectChild1)) return true;
      if (n.equals(lcaDirectChild2)) return false;
    }
    // should not happen
    return false;
  }
}
