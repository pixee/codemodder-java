package io.openpixee.java.ast;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import java.util.Optional;
import java.util.function.Predicate;
import org.javatuples.Pair;
import org.javatuples.Triplet;

/**
 * A static library for querying and returning patterns over AST nodes. Patterns are returned as
 * Optionals Tuples containing the pattern.
 */
public final class ASTPatterns {

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
        .map(p -> p instanceof VariableDeclarator ? (VariableDeclarator) p : null);
  }

  /** Test for this pattern: {@link TryStmt} -&gt; {@link VariableDeclarationExpr} ({@code vde}) */
  public static Optional<TryStmt> isResource(final VariableDeclarationExpr vde) {
    return vde.getParentNode()
        .map(p -> p instanceof TryStmt ? (TryStmt) p : null)
        .filter(ts -> ts.getResources().stream().anyMatch(rs -> rs == vde));
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
   * Test for this pattern: {@link ConstructorDeclaration} ({@code node}) -&gt; {@link Parameter}
   * -&gt; {@link SimpleName}
   *
   * @return A tuple with the above pattern in order sans the {@link SimpleName}.
   */
  public static Optional<Pair<ConstructorDeclaration, Parameter>> isConstructorFormalParameter(
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
      hasFieldDeclarationOf(final Node node, final String name) {
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
}
