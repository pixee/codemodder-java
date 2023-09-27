package io.codemodder.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import java.util.Optional;

public final class ASTTransforms {
  /** Add an import in alphabetical order. */
  public static void addImportIfMissing(final CompilationUnit cu, final String className) {
    final NodeList<ImportDeclaration> imports = cu.getImports();
    final ImportDeclaration newImport = new ImportDeclaration(className, false, false);
    if (addInOrdrerIfNeeded(className, imports, newImport)) {
      return;
    }
    cu.addImport(className);
  }

  private static boolean addInOrdrerIfNeeded(
      final String className,
      final NodeList<ImportDeclaration> imports,
      final ImportDeclaration newImport) {
    if (imports.contains(newImport)) {
      return true;
    }
    for (int i = 0; i < imports.size(); i++) {
      final ImportDeclaration existingImport = imports.get(i);

      if (existingImport.getNameAsString().compareTo(className) > 0) {
        // Workaround for a bug caused by adding imports at the top
        // It adds an extra empty line
        if (i == 0) {
          existingImport.replace(newImport);
          imports.addAfter(existingImport, newImport);
          return true;
        } else {
          imports.addBefore(newImport, existingImport);
        }
        return true;
      }
    }
    return false;
  }

  /** Add an import in alphabetical order. */
  public static void addImportIfMissing(final CompilationUnit cu, final Class<?> clazz) {
    addImportIfMissing(cu, clazz.getName());
  }

  public static void addStaticImportIfMissing(final CompilationUnit cu, final String method) {
    final NodeList<ImportDeclaration> imports = cu.getImports();
    final ImportDeclaration newMethodImport = new ImportDeclaration(method, true, false);
    if (addInOrdrerIfNeeded(method, imports, newMethodImport)) {
      return;
    }
    cu.addImport(method, true, false);
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
    // LambdaExpr (mostly due to JavaParser's grammar)
    final var parent = existingStatement.getParentNode().get();
    // In those cases we need to create a BlockStmt for
    // the existing and newly added Statement
    if (parent instanceof NodeWithBody
        || parent instanceof IfStmt
        || parent instanceof LabeledStmt
        || parent instanceof LambdaExpr) {
      final var newBody = new BlockStmt();
      existingStatement.replace(newBody);
      newBody.addStatement(newStatement);
      newBody.addStatement(existingStatement);
    } else {
      // The only option left is BlockStmt. Otherwise, existingStatement is a BlockStmt contained
      // in a:
      // NodeWithBody, MethodDeclaration, NodeWithBlockStmt, SwitchStmt(?)
      // No way (or reason) to add a Statement before those, maybe throw an Error?
      final var block = (BlockStmt) parent;
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
    final var parent = existingStatement.getParentNode().get();
    // See comments in addStatementBeforeStatement
    if (parent instanceof NodeWithBody
        || parent instanceof IfStmt
        || parent instanceof LabeledStmt
        || parent instanceof LambdaExpr) {
      final var newBody = new BlockStmt();
      existingStatement.replace(newBody);
      newBody.addStatement(existingStatement);
      newBody.addStatement(newStatement);
    } else {
      final var block = (BlockStmt) parent;
      block.getStatements().addAfter(newStatement, existingStatement);
    }
  }

  /**
   * Given a local variable declaration {@code stmt}, where {@code vdecl} is a single initialized
   * declaration of a variable {@code v} with scope {@code scope}, {@code v} is never assigned in
   * its scope, then wrap the declaration into as a resource of a try stmt.
   */
  public static TryStmt wrapIntoResource(
      final ExpressionStmt stmt, final VariableDeclarationExpr vdecl, final LocalScope scope) {
    final var wrapper = new TryStmt();
    wrapper.getResources().add(vdecl);

    final var block = new BlockStmt();
    scope
        .getStatements()
        .forEach(
            s -> {
              s.remove();
              block.addStatement(s);
            });
    wrapper.setTryBlock(block);

    stmt.replace(wrapper);

    return wrapper;
  }

  /** Given a {@link TryStmt} split its resources into two nested {@link TryStmt}s. */
  public static TryStmt splitResources(final TryStmt stmt, final int index) {
    final var resources = stmt.getResources();
    final var head = new NodeList<Expression>();
    final var tail = new NodeList<Expression>();
    for (int i = 0; i <= index; i++) head.add(resources.get(i));
    for (int i = index + 1; i < resources.size(); i++) tail.add(resources.get(i));

    stmt.setResources(head);

    final var innerTry = new TryStmt();
    innerTry.setResources(tail);
    innerTry.setTryBlock(stmt.getTryBlock());
    stmt.setTryBlock(new BlockStmt(new NodeList<>(innerTry)));

    return stmt;
  }

  /**
   * Given a {@link TryStmt} without any finally and catch clauses, and that is the first statement
   * of a try with resources block, merge the two try statements into one.
   */
  public static TryStmt combineResources(final TryStmt innerTry) {
    final var outerTry = (TryStmt) innerTry.getParentNode().flatMap(Node::getParentNode).get();
    innerTry.getResources().forEach(outerTry.getResources()::add);
    outerTry.getTryBlock().getStatements().stream()
        .skip(1)
        .forEach(innerTry.getTryBlock()::addStatement);
    outerTry.setTryBlock(innerTry.getTryBlock());
    return outerTry;
  }

  /** Remove an import if we can't find references to it in the code. */
  public static void removeImportIfUnused(final CompilationUnit cu, final String className) {
    final NodeList<ImportDeclaration> imports = cu.getImports();
    Optional<ImportDeclaration> importToRemove =
        imports.stream().filter(i -> i.getNameAsString().equals(className)).findFirst();
    if (importToRemove.isEmpty()) {
      // this wasn't imported, so there's nothing to do. maybe it's a package-protected class?
      return;
    }
    String simpleName = className.substring(className.lastIndexOf('.') + 1);
    if (cu.findAll(Node.class).stream()
        .filter(n -> n instanceof NodeWithSimpleName<?>)
        .map(n -> (NodeWithSimpleName<?>) n)
        .anyMatch(n -> n.getNameAsString().equals(simpleName))) {
      return;
    }
    if (cu.findAll(ClassOrInterfaceType.class).stream()
        .anyMatch(n -> n.getNameAsString().equals(simpleName))) {
      return;
    }
    cu.remove(importToRemove.get());
  }
}
