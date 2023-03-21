package io.openpixee.java.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.CallableDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
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
    var methodDeclarationOrNullRef = node.getParentNode();
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
    var type = node.getParentNode();
    return (ClassOrInterfaceDeclaration) type.orElse(null);
  }

  /** Return a string description of the type and method that contain this {@link Node}. */
  public static String describeType(final Node node) {
    var cu = node.findCompilationUnit().get();
    var filePackage = cu.getPackageDeclaration().orElse(new PackageDeclaration());
    var packageName = filePackage.getNameAsString();
    var type = cu.getPrimaryTypeName().orElse("unknown_type");
    return packageName.isBlank() ? type : packageName + "." + type;
  }
  /**
   * Given a {@link LocalVariableDeclaration} verifies if it is final or never assigned. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html">Java Language
   * Specification - Section 4.12.14</a> for the definitions of final variables.
   */
  public static boolean isFinalOrNeverAssigned(LocalVariableDeclaration lvd) {
    return isFinalOrNeverAssigned(lvd.getVariableDeclarator(), lvd.getScope());
  }

  /**
   * Given a {@link SimpleName} {@code name} and a {@link VariableDeclarationExpr} with a declarator
   * of {@code name}, verifies if {@code name} is final or never assigned. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html">Java Language
   * Specification - Section 4.12.14</a> for the definitions of final variables.
   */
  public static boolean isFinalOrNeverAssigned(VariableDeclarator vd, LocalVariableScope scope) {
    // Assumes vde contains a declarator with name
    var vde = (VariableDeclarationExpr) vd.getParentNode().get();
    // has final modifier
    if (vde.isFinal()) return true;

    // Effectively Final: never operand of unary operator
    Predicate<UnaryExpr> isOperand =
        ue ->
            ue.getExpression().isNameExpr()
                && ue.getExpression()
                    .asNameExpr()
                    .getName()
                    .asString()
                    .equals(vd.getName().asString());
    for (var expr : scope.getExpressions()) {
      if (expr.findFirst(UnaryExpr.class, isOperand).isPresent()) return false;
    }
    for (var stmt : scope.getStatements()) {
      if (stmt.findFirst(UnaryExpr.class, isOperand).isPresent()) return false;
    }

    Predicate<AssignExpr> isLHS =
        ae ->
            ae.getTarget().isNameExpr()
                && ae.getTarget().asNameExpr().getNameAsString().equals(vd.getNameAsString());
    if (vd.getInitializer().isPresent()) {
      for (var stmt : scope.getStatements())
        if (stmt.findFirst(AssignExpr.class, isLHS).isPresent()) return false;
      for (var expr : scope.getExpressions())
        if (expr.findFirst(AssignExpr.class, isLHS).isPresent()) return false;
      return true;
    }
    // TODO If not initialized, always definitively unassigned whenever lhs of assignment
    return false;
  }

  public static boolean isNotInitializedAndAssignedAtMostOnce(
      VariableDeclarator vd, LocalVariableScope scope) {
    Predicate<AssignExpr> isLHS =
        ae ->
            ae.getTarget().isNameExpr()
                && ae.getTarget().asNameExpr().getName().asString().equals(vd.getName().asString());
    if (vd.getInitializer().isEmpty()) {
      var allAssignments =
          Stream.concat(
              scope.getExpressions().stream()
                  .flatMap(e -> e.findAll(AssignExpr.class, isLHS).stream()),
              scope.getStatements().stream()
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
  public static LocalVariableScope findLocalVariableScope(VariableDeclarator vd) {
    var p = vd.getParentNode().get().getParentNode().get();
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

  public static ReverseEvaluationOrder reversePreOrderIterator(Node n) {
    if (n.getParentNode().isPresent()) {
      int pos = n.getParentNode().get().getChildNodes().indexOf(n);
      return new ReverseEvaluationOrder(n, pos);
    } else {
      return new ReverseEvaluationOrder(n, 0);
    }
  }

  public static final class ReverseEvaluationOrder implements Iterator<Node> {

    private Node current;
    private int posFromParent;

    ReverseEvaluationOrder(Node n, int posFromParent) {
      this.current = n;
      this.posFromParent = posFromParent;
    }

    @Override
    public Node next() {
      var parent = current.getParentNode().get();
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

  private static Optional<Node> isLocalNameSource(Node n, String name) {
    if (ASTPatterns.isExpressionStmtDeclarationOf(n, name).isPresent()
        || ASTPatterns.isResourceOf(n, name).isPresent()
        || ASTPatterns.isForVariableDeclarationOf(n, name).isPresent()
        || ASTPatterns.isForEachVariableDeclarationOf(n, name).isPresent()
        || ASTPatterns.isPatternExprDeclarationOf(n, name).isPresent()
        || ASTPatterns.isLambdaExprParameterOf(n, name).isPresent()
        || ASTPatterns.isExceptionParameterOf(n, name).isPresent()
        || ASTPatterns.isMethodFormalParameterOf(n, name).isPresent()
        || ASTPatterns.isConstructorFormalParameter(n, name).isPresent()
        || ASTPatterns.isLocalTypeDeclarationOf(n, name).isPresent()
        || ASTPatterns.isLocalRecordDeclarationOf(n, name).isPresent()) {
      return Optional.of(n);
    }
    return Optional.empty();
  }

  /**
   * Checks if there exists a non-callable member with {@code name} in an @{link
   * ClassOrInterfaceDeclaration}. If so, returns the {@link Node} with the declaration.
   */
  public static Optional<Node> isClassOrNonCallableMemberOf(
      final ClassOrInterfaceDeclaration classDecl, final String name) {
    if (classDecl.getNameAsString().equals(name)) return Optional.of(classDecl);
    for (var m : classDecl.getMembers()) {
      if (m instanceof CallableDeclaration) {
        // ignore callable
      }
      // AnnotationMemberDeclaration,  CompactConstructorDeclaration,
      // EnumConstantDeclaration, TypeDeclaration
      else if (m instanceof NodeWithSimpleName<?>) {
        System.out.println("NodeWithSimpleName");
        var named = (NodeWithSimpleName<?>) m;
        if (named.getNameAsString().equals(name)) {
          return Optional.of(m);
        }
      } else if (m instanceof FieldDeclaration) {
        var maybeVD =
            m.asFieldDeclaration().getVariables().stream()
                .filter(vd -> vd.getNameAsString().equals(name))
                .findFirst();
        if (maybeVD.isPresent()) return Optional.of(m);
      }
      // InitializerDeclaration does not introduce fields
    }
    return Optional.empty();
  }

  /**
   * Checks if there exists a enum entry with {@code name} in an @{link EnumDeclaration}. If so,
   * returns the {@link Node} with the declaration.
   */
  public static Optional<Node> isEnumMemberOf(final EnumDeclaration enumDecl, final String name) {
    return enumDecl.getEntries().stream()
        .filter(ecd -> ecd.getNameAsString().equals(name))
        .findFirst()
        .map(ecd -> ecd);
  }

  private static Optional<Node> findLocalNameSource(Node current, final String name) {
    var it = reversePreOrderIterator(current);
    while (!(current instanceof TypeDeclaration) && it.hasNext()) {
      current = it.next();
      var maybeFound = isLocalNameSource(current, name);
      if (maybeFound.isPresent()) return maybeFound;
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
   * Tries to find the declaration that originates a {@link SimpleName} use that is not a name of a
   * {@link MethodCallExpr} or {@link MethodReferenceExpr}.
   */
  public static Optional<Node> findNonCallableSimpleNameSource(final SimpleName name) {
    return findNonCallableSimpleNameSource(name, name.asString());
  }

  /**
   * starting from a {@link Node} {@code start}, tries to find the declaration that originates
   * {@code name} use that is not a name of a {@link MethodCallExpr} or {@link MethodReferenceExpr}
   * .
   */
  public static Optional<Node> findNonCallableSimpleNameSource(
      final Node start, final String name) {
    // Callable names need more context like signatures to be found. Also can be overloaded
    // TODO consider type parameters?
    // TODO consider reflexivity? class A{} -> A -> class A{}
    Node current = start;
    // Search locally first
    while (current.hasParentNode()) {
      current = current.getParentNode().get();
      // try locally first, goes reverse pre order until it reaches a type declaration
      var maybeDecl = findLocalNameSource(current, name);
      if (maybeDecl.isPresent()) {
        return maybeDecl;
      }
      // TypeDeclaration: ClassOrInterfaceDeclaration, EnumDeclaration, RecordDeclaration
      if (current instanceof ClassOrInterfaceDeclaration) {
        maybeDecl = isClassOrNonCallableMemberOf((ClassOrInterfaceDeclaration) current, name);
      }
      if (maybeDecl.isPresent()) {
        return maybeDecl;
      }
    }
    // reached CompilationUnit check for top level classes
    var topLevelTypes = current.findCompilationUnit().get().getTypes();
    var maybeDecl =
        topLevelTypes.stream().filter(t -> t.getNameAsString().equals(name)).findFirst();
    if (maybeDecl.isPresent()) return maybeDecl.map(n -> n);

    // it's either wildcard imported, inherited, or in the package namespace
    return Optional.empty();
  }

  public static Optional<LocalVariableDeclaration> findEarliestLocalDeclarationOf(
      Node start, String name) {
    var maybeParent = start.getParentNode();
    if (maybeParent.isEmpty()) return Optional.empty();
    var parent = maybeParent.get();
    if (parent instanceof BlockStmt) {
      var block = (BlockStmt) parent;
      for (var stmt : block.getStatements()) {
        if (stmt.equals(start)) break;
        var maybeExprStmtTriplet = ASTPatterns.isExpressionStmtDeclarationOf(stmt, name);
        if (maybeExprStmtTriplet.isPresent())
          return Optional.of(
              new ExpressionStmtVariableDeclaration(
                  maybeExprStmtTriplet.get().getValue0(),
                  maybeExprStmtTriplet.get().getValue1(),
                  maybeExprStmtTriplet.get().getValue2()));
      }
      return findEarliestLocalDeclarationOf(parent, name);
    } else if (parent instanceof TryStmt) {
      var maybeResource = ASTPatterns.isResourceOf(parent, name);
      if (maybeResource.isPresent()) {
        return Optional.of(
            new TryResourceDeclaration(
                maybeResource.get().getValue0(),
                maybeResource.get().getValue1(),
                maybeResource.get().getValue2()));
      }
    } else if (parent instanceof ExpressionStmt) {
      var maybeExpressionDeclaration = ASTPatterns.isExpressionStmtDeclarationOf(parent, name);
      if (maybeExpressionDeclaration.isPresent()) {
        return Optional.of(
            new ExpressionStmtVariableDeclaration(
                maybeExpressionDeclaration.get().getValue0(),
                maybeExpressionDeclaration.get().getValue1(),
                maybeExpressionDeclaration.get().getValue2()));
      }
    } else if (parent instanceof ForEachStmt) {
      var maybeForDeclaration = ASTPatterns.isForEachVariableDeclarationOf(parent, name);
      if (maybeForDeclaration.isPresent())
        return Optional.of(
            new ForEachDeclaration(
                maybeForDeclaration.get().getValue0(),
                maybeForDeclaration.get().getValue1(),
                maybeForDeclaration.get().getValue2()));
    } else if (parent instanceof ForStmt) {
      var maybeForDeclaration = ASTPatterns.isForVariableDeclarationOf(parent, name);
      if (maybeForDeclaration.isPresent())
        return Optional.of(
            new ForInitDeclaration(
                maybeForDeclaration.get().getValue0(),
                maybeForDeclaration.get().getValue1(),
                maybeForDeclaration.get().getValue2()));
    }
    return findEarliestLocalDeclarationOf(parent, name);
  }

  /**
   * Returns the unique path from {@code n} to the root of the tree. The path includes {@code n}
   * itself.
   */
  public static ArrayList<Node> pathToRoot(Node n) {
    ArrayList<Node> path = new ArrayList<>(List.of(n));
    var currentNode = n;
    while (currentNode.getParentNode().isPresent()) {
      path.add(currentNode.getParentNode().get());
      currentNode = currentNode.getParentNode().get();
    }
    return path;
  }

  /** Returns the lowest common ancestor of a pair of nodes. */
  public static Node lowestCommonAncestor(Node n1, Node n2) {
    var n1Path = pathToRoot(n1);
    var n2Path = pathToRoot(n2);
    var i1 = n1Path.size() - 1;
    var i2 = n2Path.size() - 1;
    while (n1Path.get(i1).equals(n2Path.get(i2)) && i1 >= 0 && i2 >= 0) {
      i1 -= 1;
      i2 -= 1;
    }
    return n1Path.get(i1);
  }

  /** Returns true if and only if {@code n1} &lt;= {@code n2} in post-order. */
  public static boolean postOrderLessThanOrEqual(Node n1, Node n2) {
    if (n1.equals(n2)) return true;
    var n1Path = pathToRoot(n1);
    var n2Path = pathToRoot(n2);
    var i1 = n1Path.size() - 1;
    var i2 = n2Path.size() - 1;
    while (i1 >= 0 && i2 >= 0 && n1Path.get(i1).equals(n2Path.get(i2))) {
      i1 -= 1;
      i2 -= 1;
    }
    // lowestCommonAncestor
    var lca = n1Path.get(i1 + 1);
    System.out.println(lca);
    if (lca.equals(n2)) return true;
    if (lca.equals(n1)) return false;

    var lcaDirectChild1 = n1Path.get(i1);
    var lcaDirectChild2 = n2Path.get(i2);

    for (Node n : lca.getChildNodes()) {
      if (n.equals(lcaDirectChild1)) return true;
      if (n.equals(lcaDirectChild2)) return false;
    }
    // should not happen
    return false;
  }
}
