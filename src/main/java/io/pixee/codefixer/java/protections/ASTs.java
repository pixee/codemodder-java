package io.pixee.codefixer.java.protections;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.Statement;
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
    if(methodDeclarationOrNullRef.isPresent()) {
      return Optional.of((MethodDeclaration)methodDeclarationOrNullRef.get());
    }
    return Optional.empty();
  }

  /**
   * Searches up the AST to find the {@link com.github.javaparser.ast.CompilationUnit} given {@link
   * Node}. There could be orphan statements I guess in stray Java files, so return null if we ever
   * run into that? Not sure how expected that will be, so not sure if I should make it an
   * exception-based pattern.
   */
  public static CompilationUnit findCompilationUnitFrom(Node node) {
    while (node.getParentNode().isPresent()
        && !(node.getParentNode().get() instanceof CompilationUnit)) {
      node = node.getParentNode().get();
    }
    var compilationUnit = node.getParentNode();
    return (CompilationUnit) compilationUnit.orElse(null);
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

  /**
   * This is working around a bug in the lexical parsing:
   * https://github.com/javaparser/javaparser/issues/2264
   *
   * <p>We should just be able to call:
   *
   * <p>vulnerableBlock.getStatements().addBefore(hardeningStmt, readObjectInvocation);
   *
   * <p>Unfortunately that breaks stuff because of the listeners in NodeList related to lexical
   * preservation. So we have to insert a nonsense statement in there first that looks different
   * from the method we want to add, then replace it, and that works somehow.
   */
  public static void addStatementBeforeStatement(
      final Statement existingStatement, final Statement newStatementToPrecede) {
    var vulnerableBlock = (BlockStmt) existingStatement.getParentNode().get();
    var indexOfStatementToPrecede = findIndexOf(vulnerableBlock, existingStatement);
    var ignoredExpression = new MethodCallExpr(new NameExpr("ignored"), "thisIsIgnored");
    var ignoredStatement = new ExpressionStmt(ignoredExpression);
    vulnerableBlock.getStatements().addBefore(ignoredStatement, existingStatement);
    vulnerableBlock.setStatement(indexOfStatementToPrecede, newStatementToPrecede);
  }

  private static int findIndexOf(final BlockStmt block, final Statement stmt) {
    for (int i = 0; i < block.getStatements().size(); i++) {
      if (block.getStatement(i) == stmt) {
        return i;
      }
    }
    throw new IllegalArgumentException("couldn't find node in block");
  }

  /** Return a string description of the type and method that contain this {@link Node}. */
  public static String describeType(final Node node) {
    var cu = findCompilationUnitFrom(node);
    var filePackage = cu.getPackageDeclaration().orElse(new PackageDeclaration());
    var packageName = filePackage.getNameAsString();
    var type = cu.getPrimaryTypeName().orElse("unknown_type");
    return packageName.isBlank() ? type : packageName + "." + type;
  }
}
