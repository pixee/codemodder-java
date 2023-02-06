package io.openpixee.java.plugins.codeql;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.openpixee.java.ast.ASTPatterns;
import io.openpixee.java.ast.ASTTransforms;
import io.openpixee.java.ast.ASTs;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javatuples.Triplet;

public final class JDBCResourceLeakFixer {

  public static Optional<Integer> checkAndFix(MethodCallExpr mce) {
    if (isFixable(mce)) return Optional.of(fix(mce));
    return Optional.empty();
  }

  public static boolean isFixable(MethodCallExpr mce) {
    // Assumptions/Properties from CodeQL:
    // No close() is called within its scope.
    // There does not exists a "root" expression that is closed.
    // e.g. new BufferedReader(r), r is the root, stmt.executeQuery(...), stmt is the root
    // It does not escape the contained method's scope: assigned to a field or returned.
    // It won't check for escaping of dependent/root resources for JDBC types

    // A resource R is dependent of another resource S if closing S will also close R.
    // The following suffices to check if a resource R can be closed.
    // A resource R can be closed if no dependent resource S exists s.t.:
    // (1) S is not closed within R's scope, and
    // (2) S escapes R's scope
    // We don't have the tools to actually test if a resource is closed at every possible branch, so
    // instead we only check if S escapes.
    // We can check if S is closed at some point, but that is not sufficient, consider:
    // ResultSet rs;
    // {
    //   var stmt = conn.createStatement;
    //   rs = stmt.executeQuery(query);
    //   if(condition)
    //     rs.close();
    // }
    // if(!condition)
    //   rs.getRow();

    // TODO For ResultSet objects, still need need to check if the generating *Statement object does
    // not escape/is a field/parameter, consider:
    // Statement foo(String query){
    //   var stmt = conn.createStatement();
    //   var rs = stmt.executeQuery(query);
    //   return stmt;
    // }
    // void bar(String query){
    //   var stmt = foo(query);
    //   var rs = stmt.getResultSet();
    // }

    var maybeVD = immediatelyFlowsIntoLocalVariable(mce);
    var allDependent = findDependentResources(mce);
    if (maybeVD.isPresent()) {
      var scope = ASTs.findLocalVariableScope(maybeVD.get());
      final Predicate<Node> isInScope = n -> ASTs.inScope(n, scope);
      if (allDependent.stream().anyMatch(e -> escapesRootScope(e, isInScope))) return false;
      else return true;
    } else {
      final Predicate<Node> isInScope = n -> true;
      if (allDependent.stream().anyMatch(e -> escapesRootScope(e, isInScope))) return false;
      else return true;
    }
  }

  /** Fixes the leak of {@code mce} and returns its line. */
  public static int fix(MethodCallExpr mce) {
    int originalLine = mce.getRange().get().begin.line;

    // Is LocalDeclarationStmt and Never Assigned -> Wrap as a try resource
    var maybeInit = ASTPatterns.isInitExpr(mce);
    if (maybeInit.isPresent()) {
      var trip =
          maybeInit
              .flatMap(JDBCResourceLeakFixer::isVariableOfLocalDeclarationStmt)
              .filter(
                  t ->
                      ASTs.isFinalOrNeverAssigned(
                          t.getValue2(), ASTs.findLocalVariableScope(t.getValue2())));
      if (trip.isPresent()) {
        // unpacking
        var stmt = trip.get().getValue0();
        var vde = trip.get().getValue1();
        var vd = trip.get().getValue2();
        ASTTransforms.wrapIntoResource(stmt, vde, ASTs.findLocalVariableScope(vd));
      }
    }
    return originalLine;
  }

  public static Optional<Triplet<ExpressionStmt, VariableDeclarationExpr, VariableDeclarator>>
      isVariableOfLocalDeclarationStmt(VariableDeclarator vd) {
    return vd.getParentNode()
        .map(p -> p instanceof VariableDeclarationExpr ? (VariableDeclarationExpr) p : null)
        .map(
            vde ->
                (vde.getParentNode().isPresent()
                        && vde.getParentNode().get() instanceof ExpressionStmt)
                    ? new Triplet<>((ExpressionStmt) vde.getParentNode().get(), vde, vd)
                    : null);
  }

  public static boolean isScopeInMethodCall(MethodCallExpr mce, String name) {
    return mce.getScope()
        .filter(m -> m.isNameExpr() && m.asNameExpr().getNameAsString().equals(name))
        .isPresent();
  }

  public static boolean isLocalVD(VariableDeclarator vd) {
    var maybeParent = vd.getParentNode();
    return maybeParent.filter(p -> p instanceof FieldDeclaration ? false : true).isPresent();
  }

  public static boolean isCloseableType(ObjectCreationExpr oce) {
    return oce.calculateResolvedType().isReferenceType()
        && oce.calculateResolvedType().asReferenceType().getAllAncestors().stream()
            .anyMatch(t -> t.describe().equals("java.io.Closeable"));
  }

  public static Optional<SimpleName> immediatelyFlowsIntoVariable(Expression expr) {
    var maybeInit = ASTPatterns.isInitExpr(expr);
    if (maybeInit.isPresent()) {
      return maybeInit.map(vd -> vd.getName());
    }
    return ASTPatterns.isAssigned(expr)
        .map(ae -> ae.getTarget().isNameExpr() ? ae.getTarget().asNameExpr() : null)
        .map(ae -> ae.getName());
  }

  /**
   * Checks if {@code expr} is immediately assigned to a local variable {@code v} through an
   * assignment or initializer.
   */
  public static Optional<VariableDeclarator> immediatelyFlowsIntoLocalVariable(Expression expr) {
    var maybeInit = ASTPatterns.isInitExpr(expr).filter(JDBCResourceLeakFixer::isLocalVD);
    if (maybeInit.isPresent()) {
      return maybeInit;
    }
    var maybeLVD =
        ASTPatterns.isAssigned(expr)
            .map(ae -> ae.getTarget().isNameExpr() ? ae.getTarget().asNameExpr() : null)
            .flatMap(ne -> ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString()))
            .map(t -> t.getValue2());
    return maybeLVD;
  }

  public static boolean isJDBCResourceInit(MethodCallExpr expr) {
    Predicate<MethodCallExpr> isResultSetGen =
        mce -> {
          switch (mce.getNameAsString()) {
            case "executeQuery":
            case "getResultSet":
            case "getGeneratedKeys":
              return true;
            default:
              return false;
          }
        };
    Predicate<MethodCallExpr> isStatementGen =
        mce -> {
          switch (mce.getNameAsString()) {
            case "createStatement":
            case "prepareCall":
            case "prepareStatement":
              return true;
            default:
              return false;
          }
        };
    Predicate<MethodCallExpr> isReaderGen =
        mce -> {
          switch (mce.getNameAsString()) {
            case "getCharacterStream":
            case "getNCharacterStream":
              return true;
            default:
              return false;
          }
        };
    Predicate<MethodCallExpr> isDependent = isResultSetGen.or(isStatementGen.or(isReaderGen));
    return isDependent.test(expr);
  }

  public static List<Expression> findDependentResources(Expression expr) {
    List<Expression> allDependent = new ArrayList<>();

    // immediately generates a JDBC resource e.g. <expr>.prepareStatement()
    var maybeMCE =
        ASTPatterns.isScopeInMethodCall(expr).filter(JDBCResourceLeakFixer::isJDBCResourceInit);
    if (maybeMCE.isPresent()) {
      allDependent.add(maybeMCE.get());
      allDependent.addAll(findDependentResources(maybeMCE.get()));
      return allDependent;
    }

    // immediately passed as a constructor argument for a closeable resource
    // the wrapping closeable resource is considered dependent
    var maybeOCE = isConstructorArgument(expr).filter(JDBCResourceLeakFixer::isCloseableType);
    if (maybeOCE.isPresent()) {
      allDependent.add(maybeOCE.get());
      allDependent.addAll(findDependentResources(maybeOCE.get()));
      return allDependent;
    }

    // is assigned to a local variable and ...
    var maybeVD = immediatelyFlowsIntoLocalVariable(expr);
    if (maybeVD.isPresent()) {
      var vd = maybeVD.get();
      var scope = ASTs.findLocalVariableScope(vd);
      // ... generates JDBC resource or ...
      Predicate<MethodCallExpr> vdGenerates =
          mce -> isScopeInMethodCall(mce, vd.getNameAsString()) && isJDBCResourceInit(mce);

      scope.stream()
          .flatMap(n -> n.findAll(MethodCallExpr.class, vdGenerates).stream())
          .flatMap(e -> Stream.concat(Stream.of(e), findDependentResources(e).stream()))
          .forEach(allDependent::add);

      // is wrapped by a closeable resource
      Predicate<ObjectCreationExpr> wrapsVD =
          oce ->
              oce.getArguments().stream()
                  .anyMatch(
                      e ->
                          e.isNameExpr()
                              && e.asNameExpr().getNameAsString().equals(vd.getNameAsString()));
      scope.stream()
          .flatMap(n -> n.findAll(ObjectCreationExpr.class, wrapsVD).stream())
          .forEach(allDependent::add);
    }

    return allDependent;
  }

  /**
   * Test for this pattern: {@link ObjectCreationExpr} -&gt; {@link Expression} ({@code expr}),
   * where ({@code expr}) is one of the constructor arguments.
   */
  public static Optional<ObjectCreationExpr> isConstructorArgument(Expression expr) {
    return expr.getParentNode()
        .map(p -> p instanceof ObjectCreationExpr ? (ObjectCreationExpr) p : null)
        .filter(oce -> oce.getArguments().stream().anyMatch(e -> e.equals(expr)));
  }

  /** Checks if a local variable escapes the scope of its encompassing method. */
  public static boolean escapesRootScope(VariableDeclarator vd, Predicate<Node> isInScope) {
    if (!isInScope.test(vd)) return true;
    var scope = ASTs.findLocalVariableScope(vd);

    // Returned
    Predicate<ReturnStmt> isReturned =
        rs ->
            rs.getExpression()
                .filter(
                    e ->
                        e.isNameExpr()
                            && e.asNameExpr().getNameAsString().equals(vd.getNameAsString()))
                .isPresent();
    if (scope.stream().anyMatch(n -> n.findFirst(ReturnStmt.class, isReturned).isPresent()))
      return true;

    // As an argument of a method call
    Predicate<MethodCallExpr> isCallArgument =
        mce ->
            mce.getArguments().stream()
                .anyMatch(
                    arg ->
                        arg.isNameExpr()
                            && arg.asNameExpr().getNameAsString().equals(vd.getNameAsString()));

    if (scope.stream().anyMatch(n -> n.findFirst(MethodCallExpr.class, isCallArgument).isPresent()))
      return true;

    // Assigned to a field
    // We have no tools to precisely identify a field, so we detect any assignments whose
    // target is not a local variable
    Predicate<AssignExpr> isAssigned =
        ae ->
            ae.getValue().isNameExpr()
                && ae.getValue().asNameExpr().getNameAsString().equals(vd.getNameAsString());

    var allAssignments =
        scope.stream()
            .flatMap(n -> n.findFirst(AssignExpr.class, isAssigned).stream())
            .collect(Collectors.toList());
    for (var ae : allAssignments) {
      var maybeVD = ASTs.findEarliestLocalDeclarationOf(ae, vd.getNameAsString());
      // Assigned to something that is not a local variable
      if (maybeVD.isEmpty()) return true;
      else {
        var localVD = maybeVD.get().getValue2();
        // TODO infinite recursion: this may loop: a=b; b=a;
        if (escapesRootScope(localVD, isInScope)) return true;
      }
    }

    // TODO constructor argument of another closeable object c and it escapes
    // c will be identified as a dependent resource, escape will be tested there, may be unecessary

    // TODO init expression of another variable, all
    return false;
  }

  /**
   * Checks if an object created/acessed by {@code expr} escapes the scope of its encompassing
   * method immediately.
   */
  public static boolean escapesRootScope(Expression expr, Predicate<Node> isInScope) {
    // immediately escapes or
    if (immediatelyEscapesMethodScope(expr)) return true;

    // is assigned/initialized to a variable and it escapes
    // initialized to a local variable
    if (ASTPatterns.isInitExpr(expr).filter(vd -> escapesRootScope(vd, isInScope)).isPresent())
      return true;

    // assigned
    var maybeSN = immediatelyFlowsIntoVariable(expr);
    if (maybeSN.isPresent()) {
      var name = maybeSN.get();
      var maybeLVD = ASTs.findEarliestLocalDeclarationOf(name, name.asString());
      // assigned to something that is not local (field)
      if (maybeLVD.isEmpty()) return true;
      else {
        return escapesRootScope(maybeLVD.get().getValue2(), isInScope);
      }
    }
    // TODO constructor argument of another closeable object c and it escapes
    // c will be identified as a dependent resource, escape will be tested there, may be unecessary
    return false;
  }

  /**
   * Checks if an object created/acessed by {@code expr} escapes the scope of its encompassing
   * method immediately, that is, without begin assigned.
   */
  public static boolean immediatelyEscapesMethodScope(Expression expr) {
    // Returned or argument of a MethodCallExpr
    if (ASTPatterns.isReturnExpr(expr).isPresent()
        || ASTPatterns.isArgumentOfMethodCall(expr).isPresent()) return true;

    // is the init expression of a field
    if (ASTPatterns.isInitExpr(expr).filter(vd -> JDBCResourceLeakFixer.isLocalVD(vd)).isEmpty())
      return true;

    // Is assigned to a field?
    var maybeAE = ASTPatterns.isAssigned(expr);
    if (maybeAE.isPresent()) {
      var ae = maybeAE.get();
      // Currently we have no precise way of knowing if the target is a field, thus
      // if the target is not a ExpressionName of a local variable, we consider it escaping
      Optional<NameExpr> maybeNameTarget =
          ae.getTarget().isNameExpr() ? Optional.of(ae.getTarget().asNameExpr()) : Optional.empty();
      var maybeLD =
          maybeNameTarget.flatMap(
              nameExpr ->
                  ASTs.findEarliestLocalDeclarationOf(nameExpr, nameExpr.getNameAsString()));
      if (maybeLD.isPresent()) return false;
      else return true;
    }
    return false;
  }
}
