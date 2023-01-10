package io.openpixee.java.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;
import java.util.Optional;

/** Utility methods that may be useful for many visitors. */
public final class ASTs {

  /** Add an import in alphabetical order. */
  public static void addImportIfMissing(final CompilationUnit cu, final String className) {
    NodeList<ImportDeclaration> imports = cu.getImports();
    ImportDeclaration newImport = new ImportDeclaration(className, false, false);
    if (imports.contains(newImport)) {
      return;
    }
    for (int i = 0; i < imports.size(); i++) {
      ImportDeclaration existingImport = imports.get(i);

      if (existingImport.getNameAsString().compareToIgnoreCase(className) > 0) {

        imports.addBefore(newImport, existingImport);
        return;
      }
    }
    cu.addImport(className);
  }

  /** Add an import in alphabetical order. */
  public static void addImportIfMissing(final CompilationUnit cu, final Class<?> clazz) {
    addImportIfMissing(cu, clazz.getName());
  }

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

  /**
   * Adds an {@link Statement} before another {@link Statement}. Single {@link Statement}s in the
   * body of For/Do/While/If/Labeled Statements are replaced with a {@link BlockStmt} containing
   * both statements.
   */
  public static void addStatementBeforeStatement(
      final Statement existingStatement, final Statement newStatement) {
    // See https://docs.oracle.com/javase/specs/jls/se19/html/jls-14.html#jls-14.5
    // Possible parents of a Statement that is not a BlockStmt:
    // NodeWithBody: ForStmt, ForEachStmt, DoStmt, WhileStmt
    // IfStmt, LabeledStmt
    var parent = existingStatement.getParentNode().get();
    // In those cases we need to create a BlockStmt for
    // the existing and newly added Statement
    if (parent instanceof NodeWithBody
        || parent instanceof IfStmt
        || parent instanceof LabeledStmt) {
      var newBody = new BlockStmt();
      existingStatement.replace(newBody);
      newBody.addStatement(newStatement);
      newBody.addStatement(existingStatement);
    } else {
      // The only option left is BlockStmt. Otherwise existingStatement is a BlockStmt contained
      // in a:
      // NodeWithBody, MethodDeclaration, NodeWithBlockStmt, SwitchStmt(?)
      // No way (or reason) to add a Statement before those, maybe throw an Error?
      var block = (BlockStmt) parent;
      // Workaround for a bug on LexicalPreservingPrinter (see
      // https://github.com/javaparser/javaparser/issues/3746)
      if (existingStatement.equals(block.getStatement(0))) {
        existingStatement.replace(newStatement);
        block.addStatement(1, existingStatement);
      } else {
        block.getStatements().addBefore(newStatement, existingStatement);
      }
    }
  }

  /**
   * Adds an {@link Statement} after another {@link Statement}. Single {@link Statement}s in the
   * body of For/Do/While/If/Labeled Statements are replaced with a {@link BlockStmt} containing
   * both statements.
   */
  public static void addStatementAfterStatement(
      final Statement existingStatement, final Statement newStatement) {
    var parent = existingStatement.getParentNode().get();
    // See comments in addStatementBeforeStatement
    if (parent instanceof NodeWithBody
        || parent instanceof IfStmt
        || parent instanceof LabeledStmt) {
      var newBody = new BlockStmt();
      existingStatement.replace(newBody);
      newBody.addStatement(existingStatement);
      newBody.addStatement(newStatement);
    } else {
      var block = (BlockStmt) parent;
      block.getStatements().addAfter(newStatement, existingStatement);
    }
  }

  /** Return a string description of the type and method that contain this {@link Node}. */
  public static String describeType(final Node node) {
    var cu = node.findCompilationUnit().get();
    var filePackage = cu.getPackageDeclaration().orElse(new PackageDeclaration());
    var packageName = filePackage.getNameAsString();
    var type = cu.getPrimaryTypeName().orElse("unknown_type");
    return packageName.isBlank() ? type : packageName + "." + type;
  }
}
