package io.codemodder.codemods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.resolution.declarations.ResolvedConstructorDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import java.util.List;
import java.util.Optional;

/**
 * This method holds some candidate APIs for dealing with exception management, usually in light of
 * post-processing a codemod change. After some stability is achieved here, this should be made a
 * public API.
 */
final class Exceptions {

  private Exceptions() {
    // utility class
  }

  /**
   * This method attempts to "clean up" after a method/constructor which is known to throw an
   * exception is now gone.
   *
   * <p>In this case, we may want to alter its existing try block, or maybe even if its method
   * signature.
   *
   * <p>This method is not guaranteed to be correct in situations in which there is limited type
   * resolution, like in the case of first party library information being missing.
   *
   * @param node the new or modified node
   * @param exceptionNoLongerThrownFqcn the fully qualified class name of the exception that is no
   *     longer thrown after the change to the node
   */
  static void cleanupExceptionHandling(final Node node, final String exceptionNoLongerThrownFqcn) {
    Optional<TryStmt> tryStmt = node.findAncestor(TryStmt.class);
    String exceptionSimpleName =
        exceptionNoLongerThrownFqcn.substring(exceptionNoLongerThrownFqcn.lastIndexOf('.') + 1);
    if (tryStmt.isPresent()) {
      TryStmt tryStatement = tryStmt.get();
      Node parent = tryStatement.getParentNode().get();
      List<MethodCallExpr> allOtherMethodCallsInTry =
          tryStatement.getTryBlock().findAll(MethodCallExpr.class).stream().toList();
      List<ObjectCreationExpr> allConstructorsInTry =
          tryStatement.getTryBlock().findAll(ObjectCreationExpr.class).stream().toList();
      if (anyOtherMethodsAreKnownThrowThis(
          allOtherMethodCallsInTry, allConstructorsInTry, exceptionNoLongerThrownFqcn)) {
        // we can't touch it
        return;
      }

      // if there's no other methods we think are causing this exception, let's remove the clause
      NodeList<CatchClause> catchClauses = tryStatement.getCatchClauses();
      catchClauses.removeIf(
          cc ->
              cc.getParameter().getType().isClassOrInterfaceType()
                  && cc.getParameter()
                      .getType()
                      .asClassOrInterfaceType()
                      .getNameAsString()
                      .equals(exceptionSimpleName));

      if (catchClauses.isEmpty()) {
        BlockStmt tryBlockCode = tryStatement.getTryBlock();
        parent.replace(tryStatement, tryBlockCode);
      }
    } else {
      // the method signature can have the throws clause
      Optional<MethodDeclaration> methodDeclaration = node.findAncestor(MethodDeclaration.class);
      if (methodDeclaration.isEmpty()) {
        return;
      }
      MethodDeclaration method = methodDeclaration.get();
      List<MethodCallExpr> allOtherMethodCallsInTry =
          method.findAll(MethodCallExpr.class).stream().toList();
      List<ObjectCreationExpr> allConstructorsInTry =
          method.findAll(ObjectCreationExpr.class).stream().toList();
      if (anyOtherMethodsAreKnownThrowThis(
          allOtherMethodCallsInTry, allConstructorsInTry, exceptionNoLongerThrownFqcn)) {
        // we can't touch it
        return;
      }

      NodeList<ReferenceType> thrownExceptions = method.getThrownExceptions();
      thrownExceptions.removeIf(
          referenceType ->
              referenceType.asClassOrInterfaceType().getNameAsString().equals(exceptionSimpleName));
    }
  }

  private static boolean anyOtherMethodsAreKnownThrowThis(
      final List<MethodCallExpr> allOtherMethodCallsInTry,
      final List<ObjectCreationExpr> allConstructorsInTry,
      final String exceptionFqcn) {
    // detect from the methods
    for (MethodCallExpr method : allOtherMethodCallsInTry) {
      try {
        ResolvedMethodDeclaration resolve = method.resolve();
        List<ResolvedType> methodExceptionTypes = resolve.getSpecifiedExceptions();
        for (ResolvedType methodExceptionType : methodExceptionTypes) {
          if (methodExceptionType.asReferenceType().getQualifiedName().equals(exceptionFqcn)) {
            return true;
          }
        }
      } catch (Exception e) {
        // we can't resolve the method, so we can't tell if it throws this exception
      }
    }
    // detect from the constructors
    for (ObjectCreationExpr constructor : allConstructorsInTry) {
      try {
        ResolvedConstructorDeclaration resolve = constructor.resolve();
        List<ResolvedType> constructorExceptionTypes = resolve.getSpecifiedExceptions();
        for (ResolvedType constructorExceptionType : constructorExceptionTypes) {
          if (constructorExceptionType.asReferenceType().getQualifiedName().equals(exceptionFqcn)) {
            return true;
          }
        }
      } catch (Exception e) {
        // we can't resolve the constructor, so we can't tell if it throws this exception
      }
    }
    return false;
  }
}
