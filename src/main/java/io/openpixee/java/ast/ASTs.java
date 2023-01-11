package io.openpixee.java.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.Optional;

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
    if (methodDeclarationOrNullRef.isPresent()) {
      return Optional.of((MethodDeclaration) methodDeclarationOrNullRef.get());
    }
    return Optional.empty();
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
   * Given a {@link SimpleName} {@code name} and a {@link VariableDeclarationExpr} with a declarator
   * of {@code name}, verifies if {@code name} is final or effectively final. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/html/jls-4.html">Java Language
   * Specification - Section 4.12.14</a> for the definitions of final and effectively final
   * variables.
   */
  public static boolean isFinalOrEffectivelyFinal(VariableDeclarationExpr vde, SimpleName name) {
    // Assumes vde contains a declarator with name
    var nameVariableDeclarator =
        vde.getVariables().stream()
            .filter(vd -> vd.getName().asString().equals(name.asString()))
            .findFirst()
            .get();
    if (vde.isFinal()) return true;
    if (nameVariableDeclarator.getInitializer().isEmpty()
        && isDefinitivelyAssigned(vde, nameVariableDeclarator)) return true;
    return false;
  }

  /**
   * Given a {@link VariableDeclarator} {@code vd} within the {@link VariableDeclarationExpr} {@code
   * vde} of a local declaration, verifies if it is definitively assigned. See <a
   * href="https://docs.oracle.com/javase/specs/jls/se19/ht://docs.oracle.com/javase/specs/jls/se19/html/jls-16.html">
   * Java Language Specification - Chapter 16. Definite Assignment</a> for details about when a
   * variable is definitively assigned.
   */
  public static boolean isDefinitivelyAssigned(VariableDeclarationExpr vde, VariableDeclarator vd) {
    // TODO check --v or v++ operators
    return false;
  }

  /**
   * Given a {@link VariableDeclarationExpr} {@code vde} returns the scope of the variables declared
   * within. See <a href="htts://docs.oracle.com/javase/specs/jls/se19/html/jls-6.html#jls-6.3">Java
   * Language Specification - Section 6.3 </a>} for how the scope of local declarations are defined.
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
}
