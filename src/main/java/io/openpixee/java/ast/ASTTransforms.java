package io.openpixee.java.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;

public final class ASTTransforms {
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

  /**
   * Given an a local variable declaration {@code stmt}, where {@code vdecl} is a single initialized
   * declaration of a variable {@code v} with scope {@code scope}, {@code v} is never assigned in
   * its scope, then wrap the declaration into as a resource of a try stmt.
   */
  public static TryStmt wrapIntoResource(
      ExpressionStmt stmt, VariableDeclarationExpr vdecl, LocalVariableScope scope) {
    var wrapper = new TryStmt();
    wrapper.getResources().add(vdecl);

    var block = new BlockStmt();
    scope.getStatements().stream()
        .forEach(
            s -> {
              s.remove();
              block.addStatement(s);
            });
    wrapper.setTryBlock(block);

    stmt.replace(wrapper);

    return wrapper;
  }
}
