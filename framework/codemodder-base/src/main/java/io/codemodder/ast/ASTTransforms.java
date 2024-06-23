package io.codemodder.ast;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithBody;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithStatements;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.IntStream;

public final class ASTTransforms {
  /** Add an import in alphabetical order. */
  private ASTTransforms() {
  }
  
  public static void addImportIfMissing(final CompilationUnit cu, final String className) {
    final NodeList<ImportDeclaration> imports = cu.getImports();
    final ImportDeclaration newImport = new ImportDeclaration(className, false, false);
    if (addInOrderIfNeeded(className, imports, newImport)) {
      return;
    }
    cu.addImport(className);
  }

  private static boolean addInOrderIfNeeded(
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
    if (addInOrderIfNeeded(method, imports, newMethodImport)) {
      return;
    }
    cu.addImport(method, true, false);
  }

  /**
   * Adds a statement to a node at a given index. Mostly a workaround for a weird behavior of
   * JavaParser. See https://github.com/javaparser/javaparser/issues/4370.
   */
  public static <N extends Node> void addStatementAt(
      final NodeWithStatements<N> node, final Statement stmt, final int index) {
    var newStatements = new ArrayList<Statement>();
    int i = 0;
    for (var s : node.getStatements()) {
      if (i == index) {
        newStatements.add(stmt);
      }
      newStatements.add(s);
      i++;
    }

    // rebuilds the whole statements list as to preserve proper children order.

    // workaround for maintaining indent, removes all but the first
    IntStream.range(0, node.getStatements().size() - 1)
        .forEach(j -> node.getStatements().removeLast());
    // replace the first with the new statement if needed
    if (index == 0) {
      node.getStatements().get(0).replace(stmt);
    }
    newStatements.stream().skip(1).forEach(node.getStatements()::add);
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

      int existingIndex = block.getStatements().indexOf(existingStatement);
      addStatementAt(block, newStatement, existingIndex);
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
    stmt.getComment().ifPresent(comment -> wrapper.setComment(comment));

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
    if (cu.findAll(Node.class).stream()
        .filter(n -> n instanceof NodeWithName<?>)
        .map(n -> (NodeWithName<?>) n)
        .anyMatch(n -> n.getNameAsString().equals(simpleName))) {
      return;
    }
    cu.remove(importToRemove.get());
  }

  /** Checks if a given Expression is an empty string literal or resolves to one locally. */
  private static boolean isEmptyString(final Expression expr) {
    // TODO declared as empty with one assignment
    var resolved = ASTs.resolveLocalExpression(expr);
    return resolved.isStringLiteralExpr() && resolved.asStringLiteralExpr().getValue().isEmpty();
  }

  /**
   * Removes concatenation with empty strings. For example, in : ``` String a = "some string";
   * String b = ""; a + "" + b; ``` The expression `a + "" + b` would be reduced to `a`. Returns the
   * expression without the empty concatenations.
   */
  public static Expression removeEmptyStringConcatenation(final BinaryExpr binexp) {
    if (!binexp.getOperator().equals(BinaryExpr.Operator.PLUS)) {
      return binexp;
    }
    var left = binexp.getLeft();
    var right = binexp.getRight();
    if (isEmptyString(left)) {
      if (isEmptyString(right)) {
        return new StringLiteralExpr("");
      }
      return right;
    }
    if (isEmptyString(right)) {
      if (isEmptyString(left)) {
        return new StringLiteralExpr("");
      }
      return left;
    }
    return binexp;
  }

  /** Removes all concatenations with empty strings in the given subtree. */
  public static void removeEmptyStringConcatenation(Node subtree) {
    subtree
        .findAll(BinaryExpr.class, Node.TreeTraversal.POSTORDER)
        .forEach(binexp -> binexp.replace(removeEmptyStringConcatenation(binexp)));
  }

  /** Removes unused variables. */
  public static void removeUnusedLocalVariables(final Node subtree) {
    // TODO all the other cases besides ExpressionStmt declarations
    for (final var vd : subtree.findAll(VariableDeclarator.class)) {
      var maybelvd =
          LocalVariableDeclaration.fromVariableDeclarator(vd)
              .filter(lvd -> lvd instanceof ExpressionStmtVariableDeclaration);
      if (maybelvd.isPresent()) {
        var lvd = maybelvd.get();
        var allReferences = ASTs.findAllReferences(lvd);
        // No references?
        if (allReferences.isEmpty()) {
          maybelvd.get().getStatement().remove();
        }

        // Single reference, is it a definite assignment?
        if (allReferences.size() == 1) {
          if (lvd.getVariableDeclarator().getInitializer().isEmpty()) {
            var allAssignments = ASTs.findAllAssignments(lvd).limit(2).toList();
            if (allAssignments.size() == 1) {
              var aexprStmt =
                  Optional.of(allAssignments.get(0))
                      .flatMap(Node::getParentNode)
                      .map(p -> p instanceof ExpressionStmt ? (ExpressionStmt) p : null);
              if (aexprStmt.isPresent()) {
                aexprStmt.get().remove();
                lvd.getStatement().remove();
              }
            }
          }
        }
      }
    }
  }

  private static Optional<StringLiteralExpr> removeAndReturnRightmostExpression(
      final BinaryExpr binExpr) {
    if (binExpr.getRight().isStringLiteralExpr()) {
      var right = binExpr.asBinaryExpr().getRight().asStringLiteralExpr();
      binExpr.replace(binExpr.getLeft());
      return Optional.of(right);
    }
    if (binExpr.isStringLiteralExpr()) {
      return Optional.of(binExpr.asStringLiteralExpr());
    }
    return Optional.empty();
  }

  /**
   * Given a string expression, merge any literals that are directly concatenated. This transform
   * will recurse over any Names referenced.
   */
  public static void mergeConcatenatedLiterals(final Expression e) {
    // EnclosedExpr and BinaryExpr are considered as internal nodes, so we recurse
    if (e instanceof EnclosedExpr) {
      if (calculateResolvedType(e)
          .filter(rt -> rt.describe().equals("java.lang.String"))
          .isPresent()) {
        mergeConcatenatedLiterals(e.asEnclosedExpr().getInner());
      }
    }
    // Only BinaryExpr between strings should be considered
    else if (e instanceof BinaryExpr
        && e.asBinaryExpr().getOperator().equals(BinaryExpr.Operator.PLUS)) {
      mergeConcatenatedLiterals(e.asBinaryExpr().getLeft());
      mergeConcatenatedLiterals(e.asBinaryExpr().getRight());
      var left = e.asBinaryExpr().getLeft();
      var right = e.asBinaryExpr().getRight();

      if (right.isStringLiteralExpr()) {
        if (left.isStringLiteralExpr()) {
          e.replace(
              new StringLiteralExpr(
                  left.asStringLiteralExpr().getValue() + right.asStringLiteralExpr().getValue()));
        }
        if (left.isBinaryExpr()) {
          var maybeLiteral = removeAndReturnRightmostExpression(left.asBinaryExpr());
          maybeLiteral.ifPresent(
              sl ->
                  right.replace(
                      new StringLiteralExpr(
                          sl.getValue() + right.asStringLiteralExpr().getValue())));
        }
      }

    }
    // NameExpr of String types should be recursively searched for more expressions.
    else if (e instanceof NameExpr
        && calculateResolvedType(e)
            .filter(rt -> rt.describe().equals("java.lang.String"))
            .isPresent()) {
      final var resolved = ASTs.resolveLocalExpression(e);
      if (resolved != e) {
        mergeConcatenatedLiterals(resolved);
      }
    }
  }

  private static Optional<ResolvedType> calculateResolvedType(final Expression e) {
    try {
      return Optional.of(e.calculateResolvedType());
    } catch (final RuntimeException exception) {
      return Optional.empty();
    }
  }

  /**
   * Tries to merge the given try stmt with an enveloping one. Returns the merged try stmts if
   * sucessfull.
   */
  public static Optional<TryStmt> mergeStackedTryStmts(final TryStmt tryStmt) {
    // is the parent a try statement whose single statment in its block is tryStmt?
    var maybeTryParent =
        tryStmt
            .getParentNode()
            .flatMap(p -> p.getParentNode())
            .map(p -> p instanceof TryStmt ? (TryStmt) p : null)
            .filter(
                ts ->
                    ts.getTryBlock().getStatements().size() == 1
                        && ts.getTryBlock().getStatement(0) == tryStmt);
    if (maybeTryParent.isPresent()) {
      tryStmt.remove();
      var parent = maybeTryParent.get();
      parent.getResources().addAll(tryStmt.getResources());
      parent.getTryBlock().getStatements().addAll(tryStmt.getTryBlock().getStatements());
      return Optional.of(parent);
    }
    return Optional.empty();
  }
}
