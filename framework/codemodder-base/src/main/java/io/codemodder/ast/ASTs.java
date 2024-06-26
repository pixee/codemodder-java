package io.codemodder.ast;

import static io.codemodder.javaparser.ASTExpectations.expect;

import com.github.javaparser.Position;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.TypeParameter;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/** A static library for querying and returning patterns over AST nodes. */
public final class ASTs {

  private ASTs() {}

  /** Test for this pattern: {@link ExpressionStmt} -&gt; {@link Expression} ({@code expr}). */
  public static Optional<ExpressionStmt> isExpressionStmtExpr(final Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof ExpressionStmt ? (ExpressionStmt) p : null)
        .filter(stmt -> stmt.getExpression() == expr);
  }

  /**
   * Test for this pattern: {@link AssignExpr} -&gt; {@link Expression} ({@code expr}), where
   * ({@code expr}) is the right hand side expression of the assignment.
   */
  public static Optional<AssignExpr> isAssigned(final Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof AssignExpr ? (AssignExpr) p : null)
        .filter(ae -> ae.getValue() == expr);
  }

  /** Test for this pattern: {@link VariableDeclarator} -&gt; {@link Expression} ({@code expr}) */
  public static Optional<VariableDeclarator> isInitExpr(final Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof VariableDeclarator ? (VariableDeclarator) p : null)
        .filter(vd -> vd.getInitializer().filter(init -> init == expr).isPresent());
  }

  /** Test for this pattern: {@link TryStmt} -&gt; {@link VariableDeclarationExpr} ({@code vde}) */
  public static Optional<TryStmt> isResource(final VariableDeclarationExpr vde) {
    return vde.getParentNode()
        .map(p -> p instanceof TryStmt ? (TryStmt) p : null)
        .filter(ts -> ts.getResources().stream().anyMatch(rs -> rs == vde));
  }

  /** Test for this pattern: {@link Statement} -&gt; {@link Node#getParentNode()} () */
  public static Optional<Statement> isInBlock(final Statement stmt) {
    return stmt.getParentNode().map(p -> p instanceof BlockStmt ? (BlockStmt) p : null);
  }

  /**
   * Test for this pattern: {@link TryStmt} -&gt; {@link VariableDeclarationExpr} -&gt; {@link
   * VariableDeclarator} ({@code vd})
   */
  public static Optional<Pair<TryStmt, VariableDeclarationExpr>> isResource(
      final VariableDeclarator vd) {
    return vd.getParentNode()
        .map(n -> n instanceof VariableDeclarationExpr ? (VariableDeclarationExpr) n : null)
        .flatMap(vde -> isResource(vde).map(stmt -> new Pair<>(stmt, vde)));
  }

  /**
   * Test for this pattern: {@link ForStmt} -&gt; {@link VariableDeclarationExpr} -&gt; {@link
   * VariableDeclarator} ({@code vd})
   */
  public static Optional<Pair<ForStmt, VariableDeclarationExpr>> isForInitVariable(
      final VariableDeclarator vd) {
    return vd.getParentNode()
        .map(n -> n instanceof VariableDeclarationExpr ? (VariableDeclarationExpr) n : null)
        .flatMap(
            vde ->
                vde.getParentNode()
                    .map(p -> p instanceof ForStmt ? (ForStmt) p : null)
                    .map(fs -> new Pair<>(fs, vde)));
  }

  /**
   * Test for this pattern: {@link ForEachStmt} -&gt; {@link VariableDeclarationExpr} -&gt; {@link
   * VariableDeclarator} ({@code vd})
   */
  public static Optional<Pair<ForEachStmt, VariableDeclarationExpr>> isForEachVariable(
      final VariableDeclarator vd) {
    return vd.getParentNode()
        .map(n -> n instanceof VariableDeclarationExpr ? (VariableDeclarationExpr) n : null)
        .flatMap(
            vde ->
                vde.getParentNode()
                    .map(p -> p instanceof ForEachStmt ? (ForEachStmt) p : null)
                    .map(fs -> new Pair<>(fs, vde)));
  }

  /**
   * Given an {@link Expression} {@code expr}, check if {@code expr} is the scope of a {@link
   * MethodCallExpr}.
   *
   * @return A {@link MethodCallExpr} with {@code expr} as its scope.
   */
  public static Optional<MethodCallExpr> isScopeInMethodCall(final Expression expr) {
    final var maybe = expr.getParentNode();
    return maybe
        .map(p -> p instanceof MethodCallExpr ? (MethodCallExpr) p : null)
        .filter(p -> (p.getScope().isPresent() && p.getScope().get() == expr));
  }

  /**
   * Test for this pattern: {@link VariableDeclarationExpr} ({@code node}) -&gt; {@link
   * VariableDeclarator} -&gt; {@link SimpleName} ({@code name}).
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<VariableDeclarationExpr, VariableDeclarator>>
      isVariableDeclarationExprOf(final Expression node, final String name) {
    if (node instanceof VariableDeclarationExpr) {
      final VariableDeclarationExpr vde = node.asVariableDeclarationExpr();
      return vde.getVariables().stream()
          .filter(vd -> vd.getName().asString().equals(name))
          .findFirst()
          .map(vd -> new Pair<>(vde, vd));
    } else return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ExpressionStmt} ({@code node}) -&gt; {@link
   * VariableDeclarationExpr} -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName} ({@code
   * name})
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<ExpressionStmt, VariableDeclarationExpr, VariableDeclarator>>
      isExpressionStmtDeclarationOf(final Node node, final String name) {
    if (node instanceof ExpressionStmt) {
      final var exprStmt = (ExpressionStmt) node;
      final var maybePair = isVariableDeclarationExprOf(exprStmt.getExpression(), name);
      if (maybePair.isPresent()) {
        return Optional.of(
            new Triplet<>(
                (ExpressionStmt) node, maybePair.get().getValue0(), maybePair.get().getValue1()));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ForEachStmt} ({@code node}) -&gt; {@link VariableDeclarationExpr}
   * -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName} ({@code name})
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<ForEachStmt, VariableDeclarationExpr, VariableDeclarator>>
      isForEachVariableDeclarationOf(final Node node, final String name) {
    final Predicate<VariableDeclarator> isVDOf = vd -> vd.getName().asString().equals(name);
    if (node instanceof ForEachStmt) {
      final ForEachStmt fstmt = (ForEachStmt) node;
      final var vde = fstmt.getVariable();
      final var maybeVD = vde.getVariables().stream().filter(isVDOf).findFirst();
      if (maybeVD.isPresent()) {
        return Optional.of(new Triplet<>(fstmt, vde, maybeVD.get()));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link PatternExpr} ({@code node}) -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<PatternExpr> isPatternExprDeclarationOf(
      final Node node, final String name) {
    if (node instanceof PatternExpr) {
      var pexpr = (PatternExpr) node;
      if (pexpr.getNameAsString().equals(name)) return Optional.of(pexpr);
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link LambdaExpr} ({@code node}) -&gt; {@link Parameter} -&gt; {@link
   * SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<LambdaExpr, Parameter>> isLambdaExprParameterOf(
      final Node node, final String name) {
    if (node instanceof LambdaExpr) {
      var lexpr = (LambdaExpr) node;
      for (var parameter : lexpr.getParameters()) {
        if (parameter.getNameAsString().equals(name))
          return Optional.of(new Pair<>(lexpr, parameter));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link CatchClause} ({@code node}) -&gt; {@link Parameter} -&gt; {@link
   * SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<CatchClause, Parameter>> isExceptionParameterOf(
      final Node node, final String name) {
    if (node instanceof CatchClause) {
      var catchClause = (CatchClause) node;
      if (catchClause.getParameter().getNameAsString().equals(name))
        return Optional.of(new Pair<>(catchClause, catchClause.getParameter()));
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link MethodDeclaration} ({@code node}) -&gt; {@link Parameter} -&gt;
   * {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<MethodDeclaration, Parameter>> isMethodFormalParameterOf(
      final Node node, final String name) {
    if (node instanceof MethodDeclaration) {
      var mdecl = (MethodDeclaration) node;
      for (var parameter : mdecl.getParameters()) {
        if (parameter.getNameAsString().equals(name))
          return Optional.of(new Pair<>(mdecl, parameter));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link MethodDeclaration} ({@code node}) -&gt; {@link TypeParameter}
   * -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<MethodDeclaration, TypeParameter>> isMethodTypeParameterOf(
      final Node node, final String name) {
    if (node instanceof MethodDeclaration) {
      var mdecl = (MethodDeclaration) node;
      for (var parameter : mdecl.getTypeParameters()) {
        if (parameter.getNameAsString().equals(name))
          return Optional.of(new Pair<>(mdecl, parameter));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link NodeWithSimpleName} ({@code bDecl}) -&gt; {@link SimpleName},
   * with {@code name} as the {@link SimpleName}.
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<NodeWithSimpleName<?>> isNamedMemberOf(
      final BodyDeclaration<?> bodyDecl, final String name) {
    if (bodyDecl instanceof NodeWithSimpleName<?>) {
      var nwn = (NodeWithSimpleName<?>) bodyDecl;
      if (nwn.getNameAsString().equals(name)) {
        return Optional.of(nwn);
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link FieldDeclaration} ({@code bDecl}) -&gt; {@link
   * VariableDeclarator} -&gt; {@link SimpleName}, with {@code name} as the {@link SimpleName}.
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<FieldDeclaration, VariableDeclarator>> isFieldDeclarationOf(
      final BodyDeclaration<?> bDecl, final String name) {
    if (bDecl instanceof FieldDeclaration) {
      var m = (FieldDeclaration) bDecl;
      return m.asFieldDeclaration().getVariables().stream()
          .filter(vd -> vd.getNameAsString().equals(name))
          .findFirst()
          .map(vd -> new Pair<>(m, vd));
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ClassOrInterfaceDeclaration} ({@code classDecl}) -&gt; {@link
   * TypeParameter} -&gt; {@link SimpleName}, with {@code name} as the {@link SimpleName}.
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<ClassOrInterfaceDeclaration, TypeParameter>> isClassTypeParameterOf(
      final ClassOrInterfaceDeclaration classDecl, final String name) {
    for (var parameter : classDecl.getTypeParameters()) {
      if (parameter.getNameAsString().equals(name))
        return Optional.of(new Pair<>(classDecl, parameter));
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link EnumDeclaration} ({@code enumDecl}) -&gt; {@link
   * EnumConstantDeclaration} -&gt; {@link SimpleName}, with {@code name} as the {@link SimpleName}.
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<EnumDeclaration, EnumConstantDeclaration>> isEnumConstantOf(
      final EnumDeclaration enumDecl, final String name) {
    var maybeECD =
        enumDecl.getEntries().stream()
            .filter(ecd -> ecd.getNameAsString().equals(name))
            .findFirst();
    return maybeECD.map(enumConstantDeclaration -> new Pair<>(enumDecl, enumConstantDeclaration));
  }

  /**
   * Test for this pattern: {@link ConstructorDeclaration} ({@code node}) -&gt; {@link Parameter}
   * -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<ConstructorDeclaration, Parameter>> isConstructorFormalParameterOf(
      final Node node, final String name) {
    if (node instanceof ConstructorDeclaration) {
      var mdecl = (ConstructorDeclaration) node;
      for (var parameter : mdecl.getParameters()) {
        if (parameter.getNameAsString().equals(name))
          return Optional.of(new Pair<>(mdecl, parameter));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ConstructorDeclaration} ({@code node}) -&gt; {@link
   * TypeParameter} -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<ConstructorDeclaration, TypeParameter>> isConstructorTypeParameterOf(
      final Node node, final String name) {
    if (node instanceof ConstructorDeclaration) {
      var mdecl = (ConstructorDeclaration) node;
      for (var parameter : mdecl.getTypeParameters()) {
        if (parameter.getNameAsString().equals(name))
          return Optional.of(new Pair<>(mdecl, parameter));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link LocalClassDeclarationStmt} ({@code node}) -&gt; {@link
   * ClassOrInterfaceDeclaration} -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<LocalClassDeclarationStmt, ClassOrInterfaceDeclaration>>
      isLocalTypeDeclarationOf(final Node node, final String name) {
    if (node instanceof LocalClassDeclarationStmt) {
      var stmtDecl = (LocalClassDeclarationStmt) node;
      if (stmtDecl.getClassDeclaration().getNameAsString().equals(name)) {
        return Optional.of(new Pair<>(stmtDecl, stmtDecl.getClassDeclaration()));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link LocalRecordDeclarationStmt} ({@code node}) -&gt; {@link
   * RecordDeclaration} -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<LocalRecordDeclarationStmt, RecordDeclaration>>
      isLocalRecordDeclarationOf(final Node node, final String name) {
    if (node instanceof LocalRecordDeclarationStmt) {
      var stmtDecl = (LocalRecordDeclarationStmt) node;
      if (stmtDecl.getRecordDeclaration().getNameAsString().equals(name)) {
        return Optional.of(new Pair<>(stmtDecl, stmtDecl.getRecordDeclaration()));
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ClassOrInterfaceDeclaration} ({@code node}) -&gt; {@link
   * FieldDeclaration} -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<ClassOrInterfaceDeclaration, FieldDeclaration, VariableDeclarator>>
      isClassFieldDeclarationOf(final Node node, final String name) {
    if (node instanceof ClassOrInterfaceDeclaration) {
      var classDecl = (ClassOrInterfaceDeclaration) node;
      for (var field : classDecl.getFields()) {
        for (var vd : field.getVariables()) {
          if (vd.getNameAsString().equals(name))
            return Optional.of(new Triplet<>(classDecl, field, vd));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ForStmt} ({@code node}) -&gt; {@link VariableDeclarationExpr}
   * -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<ForStmt, VariableDeclarationExpr, VariableDeclarator>>
      isForVariableDeclarationOf(final Node node, final String name) {
    final Predicate<VariableDeclarator> isVDOf = vd -> vd.getName().asString().equals(name);
    if (node instanceof ForStmt) {
      final ForStmt fstmt = (ForStmt) node;
      for (final var e : fstmt.getInitialization())
        if (e instanceof VariableDeclarationExpr) {
          final var maybeVD =
              e.asVariableDeclarationExpr().getVariables().stream().filter(isVDOf).findFirst();
          if (maybeVD.isPresent()) {
            return Optional.of(new Triplet<>(fstmt, e.asVariableDeclarationExpr(), maybeVD.get()));
          }
        }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link TryStmt} ({@code node}) -&gt; {@link VariableDeclarationExpr}
   * -&gt; {@link VariableDeclarator} -&gt; {@link SimpleName} ({@code name})
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Triplet<TryStmt, VariableDeclarationExpr, VariableDeclarator>>
      isResourceOf(final Node node, final String name) {
    if (node instanceof TryStmt) {
      final var resources = ((TryStmt) node).getResources();
      for (final var e : resources) {
        final var maybePair = isVariableDeclarationExprOf(e, name);
        if (maybePair.isPresent()) {
          return Optional.of(
              new Triplet<>(
                  (TryStmt) node, maybePair.get().getValue0(), maybePair.get().getValue1()));
        }
      }
    }
    return Optional.empty();
  }

  /**
   * Test for this pattern: {@link ReturnStmt} -&gt; {@link VariableDeclarator} -&gt; {@link
   * Expression} ({@code expr})
   */
  public static Optional<ReturnStmt> isReturnExpr(final Expression expr) {
    return expr.getParentNode().map(p -> p instanceof ReturnStmt ? (ReturnStmt) p : null);
  }

  /**
   * Test for this pattern: {@link MethodCallExpr} -&gt; {@link Expression} ({@code expr}), where
   * {@code expr} is an argument.
   */
  public static Optional<MethodCallExpr> isArgumentOfMethodCall(final Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof MethodCallExpr ? (MethodCallExpr) p : null)
        .filter(mce -> mce.getArguments().stream().anyMatch(arg -> arg == expr));
  }

  /**
   * Test for this pattern: {@link ObjectCreationExpr} -&gt; {@link Expression} ({@code expr}),
   * where {@code expr} is an argument.
   */
  public static Optional<ObjectCreationExpr> isArgumentOfObjectCreationExpression(
      final Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof ObjectCreationExpr ? (ObjectCreationExpr) p : null)
        .filter(oce -> oce.getArguments().stream().anyMatch(arg -> arg == expr));
  }

  /** Checks if {@code vd} is a local declaration. */
  public static boolean isLocalVariableDeclarator(final VariableDeclarator vd) {
    final var maybeParent = vd.getParentNode();
    return maybeParent.filter(p -> !(p instanceof FieldDeclaration)).isPresent();
  }

  /**
   * Test for this pattern: {@link ObjectCreationExpr} -&gt; {@link Expression} ({@code expr}),
   * where ({@code expr}) is one of the constructor arguments.
   */
  public static Optional<ObjectCreationExpr> isConstructorArgument(final Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof ObjectCreationExpr ? (ObjectCreationExpr) p : null)
        .filter(oce -> oce.getArguments().stream().anyMatch(e -> e == expr));
  }

  /**
   * Test for this pattern: {@link ExpressionStmt} -&gt; {@link VariableDeclarationExpr} -&gt;
   * {@link VariableDeclarator} ({@code vd}).
   *
   * @return A tuple with the above pattern.
   */
  public static Optional<Triplet<ExpressionStmt, VariableDeclarationExpr, VariableDeclarator>>
      isVariableOfLocalDeclarationStmt(final VariableDeclarator vd) {
    return vd.getParentNode()
        .map(p -> p instanceof VariableDeclarationExpr ? (VariableDeclarationExpr) p : null)
        .map(
            vde ->
                (vde.getParentNode().isPresent()
                        && vde.getParentNode().get() instanceof ExpressionStmt)
                    ? new Triplet<>((ExpressionStmt) vde.getParentNode().get(), vde, vd)
                    : null);
  }

  /**
   * Test for this pattern: {@link FieldDeclaration} -&gt; {@link VariableDeclarator} ({@code vd}.
   */
  public static Optional<FieldDeclaration> isVariableOfField(final VariableDeclarator vd) {
    return vd.getParentNode().map(n -> n instanceof FieldDeclaration ? (FieldDeclaration) n : null);
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
      final VariableDeclarator vd, final LocalScope scope) {
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
  public static List<NameExpr> findAllReferences(final LocalVariableDeclaration localDeclaration) {
    return localDeclaration.getScope().stream()
        .flatMap(
            n ->
                n
                    .findAll(
                        NameExpr.class,
                        ne -> ne.getNameAsString().equals(localDeclaration.getName()))
                    .stream())
        .filter(
            ne ->
                findNonCallableSimpleNameSource(ne.getName())
                    .filter(n -> n == localDeclaration.getVariableDeclarator())
                    .isPresent())
        .collect(Collectors.toList());
  }

  /**
   * Tries to find the source of an expression if it can be uniquely defined, otherwise, returns
   * self.
   */
  public static Expression resolveLocalExpression(final Expression expr) {
    // If this is a name, find its local declaration first
    var maybelvd =
        Optional.of(expr)
            .map(e -> e instanceof NameExpr ? e.asNameExpr() : null)
            .flatMap(n -> ASTs.findEarliestLocalDeclarationOf(n.getName()))
            .map(s -> s instanceof LocalVariableDeclaration ? (LocalVariableDeclaration) s : null);
    List<AssignExpr> first2Assignments =
        maybelvd.stream().flatMap(ASTs::findAllAssignments).limit(2).toList();

    var maybeInit =
        maybelvd.flatMap(
            lvd -> lvd.getVariableDeclarator().getInitializer().map(ASTs::resolveLocalExpression));
    // No assignments and a init
    if (maybeInit.isPresent() && first2Assignments.isEmpty()) {
      return maybeInit.get();
    }

    // No init but a single assignment?
    if (maybeInit.isEmpty() && first2Assignments.size() == 1) {
      return resolveLocalExpression(first2Assignments.get(0).getValue());
    }

    // failing that, return itself
    return expr;
  }

  /**
   * Test for this pattern: {@link AssignExpr} ({@code assignExpr}) -&gt; {@link NameExpr}, where
   * ({@code expr}) is the left hand side of the assignment.
   */
  public static Optional<NameExpr> hasNamedTarget(final AssignExpr assignExpr) {
    return Optional.of(assignExpr.getTarget()).map(e -> e.isNameExpr() ? e.asNameExpr() : null);
  }

  /** Finds all assignments of a local variable declaration */
  public static Stream<AssignExpr> findAllAssignments(final LocalVariableDeclaration lvd) {
    final Predicate<AssignExpr> isLHS =
        ae ->
            ae.getTarget().isNameExpr()
                && ae.getTarget()
                    .asNameExpr()
                    .getNameAsString()
                    .equals(lvd.getDeclaration().getNameAsString());

    var streamAssignments =
        Stream.concat(
            lvd.getScope().getExpressions().stream()
                .flatMap(e -> e.findAll(AssignExpr.class, isLHS).stream()),
            lvd.getScope().getStatements().stream()
                .flatMap(s -> s.findAll(AssignExpr.class, isLHS).stream()));

    // we must check for shadowing first
    // local declarations can only be shadowed by other local declarations inside local class/record
    // declarations
    // we quickly check first if any local class/record declarations are present in the scope
    final var maybeLocalClassDeclaration =
        lvd.getScope().getStatements().stream()
            .flatMap(n -> n.findAll(LocalClassDeclarationStmt.class).stream().findAny().stream());
    final var maybeLocalRecordDeclaration =
        lvd.getScope().getStatements().stream()
            .flatMap(n -> n.findAll(LocalRecordDeclarationStmt.class).stream().findAny().stream());
    if (maybeLocalClassDeclaration.findAny().isEmpty()
        && maybeLocalRecordDeclaration.findAny().isEmpty()) {
      return streamAssignments;
    }

    // Having detected that, we resolve each name and check if it matches the declaration
    // This is mildly expensive, hence the previous check
    final Predicate<AssignExpr> resolvesToLVD =
        ae ->
            hasNamedTarget(ae)
                .flatMap(name -> findNonCallableSimpleNameSource(name.getName()))
                .filter(source -> source == lvd.getVariableDeclarator())
                .isPresent();

    return streamAssignments.filter(resolvesToLVD);
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
   * Starting from the {@link Node} {@code start}, checks if there exists a local variable
   * declaration whose name is {@code name}.
   */
  public static Optional<LocalVariableDeclaration> findEarliestLocalVariableDeclarationOf(
      final Node start, final String name) {
    return NameResolver.findLocalVariableDeclarationOf(start, name);
  }

  /**
   * Starting from the {@link Node} {@code start}, checks if there exists a local variable
   * declaration whose name is {@code name}.
   */
  public static Optional<LocalDeclaration> findEarliestLocalDeclarationOf(final SimpleName name) {
    return NameResolver.findLocalDeclarationOf(name, name.asString());
  }

  /**
   * Starting from the {@link Node} {@code start}, checks if there exists a local variable
   * declaration whose name is {@code name}.
   */
  public static Optional<LocalDeclaration> findEarliestLocalDeclarationOf(
      final Node start, final String name) {
    return NameResolver.findLocalDeclarationOf(start, name);
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
   * This finds all methods that match the given location, with the given name, and is assigned to a
   * variable of one of the given types.
   */
  public static List<MethodCallExpr> findMethodCallsWhichAreAssignedToType(
      final CompilationUnit cu,
      final int line,
      final Integer column,
      final String methodName,
      final List<String> assignedToTypes) {
    List<MethodCallExpr> candidateMethods =
        cu.findAll(MethodCallExpr.class).stream()
            .filter(
                m ->
                    m.getRange()
                        .isPresent()) // this may be true of nodes we've inserted in a previous
            // fix
            .filter(m -> m.getRange().get().begin.line == line)
            .toList();

    if (column != null) {
      Position reportedPosition = new Position(line, column);
      candidateMethods =
          candidateMethods.stream()
              .filter(m -> m.getRange().get().contains(reportedPosition))
              .toList();
    }

    candidateMethods =
        candidateMethods.stream()
            .filter(m -> methodName.equals(m.getNameAsString()))
            .filter(
                m -> {
                  Optional<VariableDeclarator> newFactoryVariableRef =
                      expect(m).toBeMethodCallExpression().initializingVariable().result();
                  if (newFactoryVariableRef.isEmpty()) {
                    return false;
                  }
                  String type = newFactoryVariableRef.get().getTypeAsString();
                  return assignedToTypes.contains(type)
                      || assignedToTypes.stream().anyMatch(type::endsWith);
                })
            .toList();
    return candidateMethods;
  }
}
