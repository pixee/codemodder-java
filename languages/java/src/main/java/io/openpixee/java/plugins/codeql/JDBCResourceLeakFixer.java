package io.openpixee.java.plugins.codeql;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import io.openpixee.java.ast.ASTPatterns;
import io.openpixee.java.ast.ASTTransforms;
import io.openpixee.java.ast.ASTs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.javatuples.Triplet;

public final class JDBCResourceLeakFixer {

  public static Optional<Integer> checkAndFix(MethodCallExpr mce) {
    if (isFixable(mce)) return tryToFix(mce);
    return Optional.empty();
  }

  public static boolean isFixable(MethodCallExpr mce) {
    // Assumptions/Properties from CodeQL:
    // No close() is called within its scope.
    // There does not exist a "root" expression that is closed.
    // e.g. new BufferedReader(r), r is the root, stmt.executeQuery(...), stmt is the root
    // It does not escape the contained method's scope: assigned to a field or returned.
    // It won't check for escaping of dependent/root resources for JDBC types

    // A resource R is dependent of another resource S if closing S will also close R.
    // The following suffices to check if a resource R can be closed.
    // (*) A resource R can be closed if no dependent resource S exists s.t.:
    // (1) S is not closed within R's scope, and
    // (2) S escapes R's scope
    // Currently, we cannot test (*), as it requires some dataflow analysis, instead we test for
    // (+): S is assigned to a variable that escapes.

    // For ResultSet objects, still need to check if the generating *Statement object does
    // not escape due to the getResultSet() method.
    try {
      if (mce.calculateResolvedType().describe().equals("java.sql.ResultSet")) {
        // should always exist and is a *Statement object
        var mScope = mce.getScope().get();
        if (mScope.isFieldAccessExpr()) return false;
        if (mScope.isNameExpr()) {
          var maybeLVD =
              ASTs.findEarliestLocalDeclarationOf(mScope, mScope.asNameExpr().getNameAsString());

          if (maybeLVD.filter(trip -> escapesRootScope(trip.getValue2(), n -> true)).isPresent())
            return false;
        }
      }
    }
    // There's a possible bug on solving types of var declarations
    catch (UnsolvedSymbolException e) {
      return false;
    }

    var maybeVD = immediatelyFlowsIntoLocalVariable(mce);
    var allDependent = findDependentResources(mce);
    if (maybeVD.isPresent()) {
      var scope = ASTs.findLocalVariableScope(maybeVD.get());
      final Predicate<Node> isInScope = n -> ASTs.inScope(n, scope);
      return allDependent.stream().noneMatch(e -> escapesRootScope(e, isInScope));
    } else {
      final Predicate<Node> isInScope = n -> true;
      return allDependent.stream().noneMatch(e -> escapesRootScope(e, isInScope));
    }
  }

  /** Tries to fix the leak of {@code mce} and returns its line. */
  public static Optional<Integer> tryToFix(MethodCallExpr mce) {
    int originalLine = mce.getRange().get().begin.line;

    // Is LocalDeclarationStmt and Never Assigned -> Wrap as a try resource
    var maybeInit = ASTPatterns.isInitExpr(mce);
    if (maybeInit.isPresent()) {
      var trip =
          maybeInit
              .flatMap(ASTPatterns::isVariableOfLocalDeclarationStmt)
              .filter(
                  t ->
                      ASTs.isFinalOrNeverAssigned(
                          t.getValue2(), ASTs.findLocalVariableScope(t.getValue2())));
      if (trip.isPresent()) {
        // unpacking
        var stmt = trip.get().getValue0();
        var vde = trip.get().getValue1();
        var vd = trip.get().getValue2();
        if (vde.getVariables().size() == 1) {
          ASTTransforms.wrapIntoResource(stmt, vde, ASTs.findLocalVariableScope(vd));
          return Optional.of(originalLine);
        }
      }
    }
    // other cases here...
    return Optional.empty();
  }

  /**
   * Given a list of {@link AssignExpr} separate it into assignments to local variables and
   * everything else.
   */
  private static Pair<List<VariableDeclarator>, List<Node>> separateAE(
      List<AssignExpr> allAE, HashSet<VariableDeclarator> memory) {
    var allLVD = new ArrayList<VariableDeclarator>();
    var notLVD = new ArrayList<Node>();
    for (var ae : allAE) {
      if (ae.getTarget().isNameExpr()) {
        var ne = ae.getTarget().asNameExpr();
        var maybeLVD =
            ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString()).map(Triplet::getValue2);
        maybeLVD.ifPresentOrElse(
            vd -> {
              if (!memory.contains(vd)) allLVD.add(vd);
            },
            () -> notLVD.add(ne));
      } else notLVD.add(ae.getTarget());
    }
    return new Pair<>(allLVD, notLVD);
  }

  private static Pair<List<VariableDeclarator>, List<Node>> combine(
      Pair<List<VariableDeclarator>, List<Node>> left,
      Pair<List<VariableDeclarator>, List<Node>> right) {
    left.getValue0().addAll(right.getValue0());
    left.getValue1().addAll(right.getValue1());
    return left;
  }

  /**
   * Finds a superset of all the variables that {@code expr} will be assigned to. The search works
   * recursively, meaning if, for example, {@code b = expr}; {@code a = b};, {@code a} will be on
   * the list.
   *
   * @return A pair {@code (lvd,lnv)} where {@code lvd} are all the {@link VariableDeclarator}s of
   *     local variables that {@code expr} is assigned to, and {@code lnv} contains all the other
   *     assignments.
   */
  public static Pair<List<VariableDeclarator>, List<Node>> flowsInto(Expression expr) {
    // is immediately assigned as an init expr
    var maybeInit = ASTPatterns.isInitExpr(expr);
    if (maybeInit.isPresent()) {
      var vd = maybeInit.get();
      if (ASTPatterns.isLocalVD(vd)) return flowsInto(vd);
      else return new Pair<>(Collections.emptyList(), List.of(vd));
    }

    // is immediately assigned
    var maybeAE = ASTPatterns.isAssigned(expr);
    if (maybeAE.isPresent()) {
      var ae = maybeAE.get();
      if (ae.getTarget().isNameExpr()) {
        var maybeLVD =
            ASTs.findEarliestLocalDeclarationOf(
                ae.getTarget(), ae.getTarget().asNameExpr().getNameAsString());
        if (maybeLVD.isPresent()) return flowsInto(maybeLVD.get().getValue2());
      }
      return new Pair<>(Collections.emptyList(), List.of(ae.getTarget()));
    }
    return new Pair<>(Collections.emptyList(), Collections.emptyList());
  }

  public static Pair<List<VariableDeclarator>, List<Node>> flowsInto(VariableDeclarator vd) {
    return flowsIntoImpl(vd, new HashSet<>());
  }

  private static Pair<List<VariableDeclarator>, List<Node>> flowsIntoImpl(
      VariableDeclarator vd, HashSet<VariableDeclarator> memory) {
    if (memory.contains(vd)) return new Pair<>(Collections.emptyList(), Collections.emptyList());
    else memory.add(vd);

    var scope = ASTs.findLocalVariableScope(vd);
    Predicate<AssignExpr> isRHSOfAE =
        ae ->
            ae.getValue().isNameExpr()
                && ae.getValue().asNameExpr().getNameAsString().equals(vd.getNameAsString());

    // assignments i.e. a = b; but not a = a
    var allAE =
        scope.stream()
            .flatMap(n -> n.findAll(AssignExpr.class, isRHSOfAE).stream())
            .filter(
                ae ->
                    !(ae.getTarget().isNameExpr()
                        && ae.getTarget()
                            .asNameExpr()
                            .getNameAsString()
                            .equals(vd.getNameAsString())))
            .collect(Collectors.toList());
    // separate the assignments to local variables from the others
    var separated = separateAE(allAE, memory);

    // init expressions var a = b;
    Predicate<VariableDeclarator> isRHSOfVD =
        varDecl ->
            varDecl
                .getInitializer()
                .filter(
                    init ->
                        init.isNameExpr()
                            && init.asNameExpr().getNameAsString().equals(vd.getNameAsString()))
                .isPresent();

    scope.stream()
        .flatMap(n -> n.findAll(VariableDeclarator.class, isRHSOfVD).stream())
        .forEach(separated.getValue0()::add);

    var pair =
        new Pair<List<VariableDeclarator>, List<Node>>(
                new ArrayList<>(), new ArrayList<>());
    for (var lvd : separated.getValue0()) {
      pair = combine(pair, flowsIntoImpl(lvd, memory));
    }
    return combine(separated, pair);
  }

  /** Checks if {@code name} is contained in a {@link NameExpr} that is the scope of {@code mce}. */
  private static boolean isScopeInMethodCall(MethodCallExpr mce, String name) {
    return mce.getScope()
        .filter(m -> m.isNameExpr() && m.asNameExpr().getNameAsString().equals(name))
        .isPresent();
  }

  /** Checks if the created object implements the {@link java.io.Closeable} interface. */
  private static boolean isCloseableType(ObjectCreationExpr oce) {
    return oce.calculateResolvedType().isReferenceType()
        && oce.calculateResolvedType().asReferenceType().getAllAncestors().stream()
            .anyMatch(t -> t.describe().equals("java.io.Closeable"));
  }

  /** Checks if {@code expr} creates a JDBC Resource. */
  private static boolean isJDBCResourceInit(MethodCallExpr expr) {
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

  /**
   * Checks if {@code expr} is immediately assigned to a local variable {@code v} through an
   * assignment or initializer.
   */
  private static Optional<VariableDeclarator> immediatelyFlowsIntoLocalVariable(Expression expr) {
    var maybeInit = ASTPatterns.isInitExpr(expr).filter(ASTPatterns::isLocalVD);
    if (maybeInit.isPresent()) {
      return maybeInit;
    }
    return ASTPatterns.isAssigned(expr)
        .map(ae -> ae.getTarget().isNameExpr() ? ae.getTarget().asNameExpr() : null)
        .flatMap(ne -> ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString()))
        .map(Triplet::getValue2);
  }

  /**
   * Find all the dependent resources of {@code expr}. A resource R is dependent if closing {@code
   * expr} will also close R.
   */
  private static List<Expression> findDependentResources(Expression expr) {
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
    var maybeOCE =
        ASTPatterns.isConstructorArgument(expr).filter(JDBCResourceLeakFixer::isCloseableType);
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
   * Checks if an object created/accessed by {@code expr} escapes the scope of its encompassing
   * method immediately, that is, without being assigned. It escapes if it is assigned to a field,
   * returned, or is the argument of a method call.
   */
  private static boolean immediatelyEscapesMethodScope(Expression expr) {
    // Returned or argument of a MethodCallExpr
    if (ASTPatterns.isReturnExpr(expr).isPresent()
        || ASTPatterns.isArgumentOfMethodCall(expr).isPresent()) return true;

    // is the init expression of a field
    if (ASTPatterns.isInitExpr(expr).filter(ASTPatterns::isLocalVD).isEmpty()) return true;

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
      return maybeLD.isEmpty();
    }
    return false;
  }

  /** Returns true if {@code vd} is returned or is an argument of a method call. */
  private static boolean escapesRootScope(VariableDeclarator vd, Predicate<Node> isInScope) {
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

    return scope.stream().anyMatch(n -> n.findFirst(MethodCallExpr.class, isCallArgument).isPresent());
  }

  /** Returns true if {@code expr} itself escapes or flows into a variable that escapes. */
  private static boolean escapesRootScope(Expression expr, Predicate<Node> isInScope) {
    if (immediatelyEscapesMethodScope(expr)) return true;
    // find all the variables it flows into
    var pair = flowsInto(expr);
    // flows into anything that is not a local variable
    if (!pair.getValue1().isEmpty()) return true;

    var allVD = pair.getValue0();
    return allVD.stream().anyMatch(vd -> escapesRootScope(vd, isInScope));
  }
}
