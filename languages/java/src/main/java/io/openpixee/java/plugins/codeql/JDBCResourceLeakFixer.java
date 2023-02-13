package io.openpixee.java.plugins.codeql;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import io.openpixee.java.ast.ASTPatterns;
import io.openpixee.java.ast.ASTTransforms;
import io.openpixee.java.ast.ASTs;
import io.openpixee.java.ast.ExpressionStmtVariableDeclaration;
import io.openpixee.java.ast.ForEachDeclaration;
import io.openpixee.java.ast.ForInitDeclaration;
import io.openpixee.java.ast.LocalVariableDeclaration;
import io.openpixee.java.ast.TryResourceDeclaration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javatuples.Pair;

/**
 * A library that contains methods for automatically fixing resource leaks detected by CodeQL's rule
 * "java/database-resource-leak" whenever possible.
 */
public final class JDBCResourceLeakFixer {

  /**
   * Detects if a {@link MethodCallExpr} of a JDBC resource type is fixable and tries to fix it.
   * Combines {@code isFixable} and {@code tryToFix}.
   */
  public static Optional<Integer> checkAndFix(final MethodCallExpr mce) {
    if (isFixable(mce)) return tryToFix(mce);
    return Optional.empty();
  }

  /**
   * Detects if a {@link MethodCallExpr} of a leaking JDBC resource type detected by CodeQL is
   * fixable. The following can be assumed.
   *
   * <p>(1) No close() is called within its scope.
   *
   * <p>(2) There does not exist a "root" expression that is closed. (e.g. new BufferedReader(r), r
   * is the root, stmt.executeQuery(...), stmt is the root).
   *
   * <p>(3) It does not escape the contained method's scope. That is, it is not assigned to a field
   * or returned.
   *
   * <p>A resource R is dependent of another resource S if closing S will also close R. The
   * following suffices to check if a resource R can be closed. (*) A resource R can be closed if no
   * dependent resource S exists s.t.: (i) S is not closed within R's scope, and (ii) S escapes R's
   * scope Currently, we cannot test (*), as it requires some dataflow analysis, instead we test for
   * (+): S is assigned to a variable that escapes.
   */
  public static boolean isFixable(final MethodCallExpr mce) {

    // For ResultSet objects, still need to check if the generating *Statement object does
    // not escape due to the getResultSet() method.
    try {
      if (mce.calculateResolvedType().describe().equals("java.sql.ResultSet")) {
        // should always exist and is a *Statement object
        final var mceScope = mce.getScope().get();
        if (mceScope.isFieldAccessExpr()) return false;
        if (mceScope.isNameExpr()) {
          final var maybeLVD =
              ASTs.findEarliestLocalDeclarationOf(
                  mceScope, mceScope.asNameExpr().getNameAsString());

          if (maybeLVD.filter(lvd -> escapesRootScope(lvd, n -> true)).isPresent()) return false;
        }
      }
    }
    // There's a possible bug on solving types of var declarations
    catch (final UnsolvedSymbolException e) {
      return false;
    }

    final var maybeLVD = immediatelyFlowsIntoLocalVariable(mce);
    final var allDependent = findDependentResources(mce);
    if (maybeLVD.isPresent()) {
      final var scope = maybeLVD.get().getScope();
      final Predicate<Node> isInScope = scope::inScope;
      return allDependent.stream().noneMatch(e -> escapesRootScope(e, isInScope));
    } else {
      final Predicate<Node> isInScope = n -> true;
      return allDependent.stream().noneMatch(e -> escapesRootScope(e, isInScope));
    }
  }

  /** Tries to fix the leak of {@code mce} and returns its line if it does. */
  public static Optional<Integer> tryToFix(final MethodCallExpr mce) {
    final int originalLine = mce.getRange().get().begin.line;

    // Is LocalDeclarationStmt and Never Assigned -> Wrap as a try resource
    final var maybeInit = ASTPatterns.isInitExpr(mce);
    if (maybeInit.isPresent()) {
      final var trip =
          maybeInit
              .flatMap(ASTPatterns::isVariableOfLocalDeclarationStmt)
              .filter(
                  t ->
                      ASTs.isFinalOrNeverAssigned(
                          t.getValue2(), ASTs.findLocalVariableScope(t.getValue2())));
      if (trip.isPresent()) {
        // unpacking for clarity
        final var stmt = trip.get().getValue0();
        final var vde = trip.get().getValue1();
        final var vd = trip.get().getValue2();
        if (vde.getVariables().size() == 1) {
          ASTTransforms.wrapIntoResource(stmt, vde, ASTs.findLocalVariableScope(vd));
          return Optional.of(originalLine);
        }
        // if vde is multiple declarations, extract the relevant vd and wrap it
      }
    }
    // other cases here...
    return Optional.empty();
  }

  private static LocalVariableDeclaration fromVariableDeclarator(VariableDeclarator vd) {
    var vde = (VariableDeclarationExpr) vd.getParentNode().get();
    var stmt = (Statement) vde.getParentNode().get();
    if (stmt instanceof TryStmt) return new TryResourceDeclaration(stmt.asTryStmt(), vde, vd);
    if (stmt instanceof ExpressionStmt)
      return new ExpressionStmtVariableDeclaration(stmt.asExpressionStmt(), vde, vd);
    if (stmt instanceof ForEachStmt) return new ForEachDeclaration(stmt.asForEachStmt(), vde, vd);
    if (stmt instanceof ForStmt) return new ForInitDeclaration(stmt.asForStmt(), vde, vd);
    return null;
  }

  /**
   * Given a list of {@link AssignExpr} separate it into assignments to local variables and
   * everything else.
   */
  private static Pair<List<LocalVariableDeclaration>, List<Node>> separateAssignExpressions(
      final List<AssignExpr> allAE, final HashSet<VariableDeclarator> memory) {
    final var allLVD = new ArrayList<LocalVariableDeclaration>();
    final var notLVD = new ArrayList<Node>();
    for (final var ae : allAE) {
      if (ae.getTarget().isNameExpr()) {
        final var ne = ae.getTarget().asNameExpr();
        final var maybeLVD = ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString());
        maybeLVD.ifPresentOrElse(
            lvd -> {
              if (!memory.contains(lvd.getVariableDeclarator())) allLVD.add(lvd);
            },
            () -> notLVD.add(ne));
      } else notLVD.add(ae.getTarget());
    }
    return new Pair<>(allLVD, notLVD);
  }

  private static Pair<List<LocalVariableDeclaration>, List<Node>> combine(
      final Pair<List<LocalVariableDeclaration>, List<Node>> left,
      final Pair<List<LocalVariableDeclaration>, List<Node>> right) {
    left.getValue0().addAll(right.getValue0());
    left.getValue1().addAll(right.getValue1());
    return left;
  }

  /**
   * Finds a superset of all the local variables that {@code expr} will be assigned to. The search
   * works recursively, meaning if, for example, {@code b = expr; a = b;}, {@code a} will be on the
   * list.
   *
   * @return A pair {@code (lvd,lnv)} where {@code lvd} are all the {@link
   *     LocalVariableDeclaration}s that {@code expr} is assigned to, and {@code lnv} contains all
   *     the other assignments.
   */
  public static Pair<List<LocalVariableDeclaration>, List<Node>> flowsInto(final Expression expr) {
    // is immediately assigned as an init expr
    final var maybeInit = ASTPatterns.isInitExpr(expr);
    if (maybeInit.isPresent()) {
      final var vd = maybeInit.get();
      if (ASTPatterns.isLocalVariableDeclarator(vd)) return flowsInto(fromVariableDeclarator(vd));
      else return new Pair<>(Collections.emptyList(), List.of(vd));
    }

    // is immediately assigned
    final var maybeAE = ASTPatterns.isAssigned(expr);
    if (maybeAE.isPresent()) {
      final var ae = maybeAE.get();
      if (ae.getTarget().isNameExpr()) {
        final var maybeLVD =
            ASTs.findEarliestLocalDeclarationOf(
                ae.getTarget(), ae.getTarget().asNameExpr().getNameAsString());
        if (maybeLVD.isPresent()) return flowsInto(maybeLVD.get());
      }
      return new Pair<>(Collections.emptyList(), List.of(ae.getTarget()));
    }
    return new Pair<>(Collections.emptyList(), Collections.emptyList());
  }

  public static Pair<List<LocalVariableDeclaration>, List<Node>> flowsInto(
      final LocalVariableDeclaration lvd) {
    return flowsIntoImpl(lvd, new HashSet<>());
  }

  private static Pair<List<LocalVariableDeclaration>, List<Node>> flowsIntoImpl(
      final LocalVariableDeclaration lvd, final HashSet<VariableDeclarator> memory) {
    if (memory.contains(lvd.getVariableDeclarator()))
      return new Pair<>(Collections.emptyList(), Collections.emptyList());
    else memory.add(lvd.getVariableDeclarator());

    final var scope = lvd.getScope();
    final Predicate<AssignExpr> isRHSOfAE =
        ae ->
            ae.getValue().isNameExpr()
                && ae.getValue().asNameExpr().getNameAsString().equals(lvd.getName());

    // assignments i.e. a = b; but not a = a
    final var allAE =
        scope.stream()
            .flatMap(n -> n.findAll(AssignExpr.class, isRHSOfAE).stream())
            .filter(
                ae ->
                    !(ae.getTarget().isNameExpr()
                        && ae.getTarget().asNameExpr().getNameAsString().equals(lvd.getName())))
            .collect(Collectors.toList());
    // separate the assignments to local variables from the others
    final var separated = separateAssignExpressions(allAE, memory);

    // init expressions var a = b;
    final Predicate<VariableDeclarator> isRHSOfVD =
        varDecl ->
            varDecl
                .getInitializer()
                .filter(
                    init ->
                        init.isNameExpr()
                            && init.asNameExpr().getNameAsString().equals(lvd.getName()))
                .isPresent();

    scope.stream()
        .flatMap(n -> n.findAll(VariableDeclarator.class, isRHSOfVD).stream())
        .forEach(vd -> separated.getValue0().add(fromVariableDeclarator(vd)));

    var pair =
        new Pair<List<LocalVariableDeclaration>, List<Node>>(new ArrayList<>(), new ArrayList<>());
    for (final var lvdInSep : separated.getValue0()) {
      pair = combine(pair, flowsIntoImpl(lvdInSep, memory));
    }
    return combine(separated, pair);
  }

  /** Checks if {@code name} is contained in a {@link NameExpr} that is the scope of {@code mce}. */
  private static boolean isScopeInMethodCall(final MethodCallExpr mce, final String name) {
    return mce.getScope()
        .filter(m -> m.isNameExpr() && m.asNameExpr().getNameAsString().equals(name))
        .isPresent();
  }

  /** Checks if the created object implements the {@link java.io.Closeable} interface. */
  private static boolean isCloseableType(final ObjectCreationExpr oce) {
    return oce.calculateResolvedType().isReferenceType()
        && oce.calculateResolvedType().asReferenceType().getAllAncestors().stream()
            .anyMatch(t -> t.describe().equals("java.io.Closeable"));
  }

  /** Checks if {@code expr} creates a JDBC Resource. */
  private static boolean isJDBCResourceInit(final MethodCallExpr expr) {
    final Predicate<MethodCallExpr> isResultSetGen =
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
    final Predicate<MethodCallExpr> isStatementGen =
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
    final Predicate<MethodCallExpr> isReaderGen =
        mce -> {
          switch (mce.getNameAsString()) {
            case "getCharacterStream":
            case "getNCharacterStream":
              return true;
            default:
              return false;
          }
        };
    final Predicate<MethodCallExpr> isDependent = isResultSetGen.or(isStatementGen.or(isReaderGen));
    return isDependent.test(expr);
  }

  /**
   * Checks if {@code expr} is immediately assigned to a local variable {@code v} through an
   * assignment or initializer.
   */
  private static Optional<LocalVariableDeclaration> immediatelyFlowsIntoLocalVariable(
      final Expression expr) {
    final var maybeInit =
        ASTPatterns.isInitExpr(expr).filter(ASTPatterns::isLocalVariableDeclarator);
    if (maybeInit.isPresent()) {
      return maybeInit.map(JDBCResourceLeakFixer::fromVariableDeclarator);
    }
    return ASTPatterns.isAssigned(expr)
        .map(ae -> ae.getTarget().isNameExpr() ? ae.getTarget().asNameExpr() : null)
        .flatMap(ne -> ASTs.findEarliestLocalDeclarationOf(ne, ne.getNameAsString()));
  }

  /**
   * Find all the dependent resources of {@code expr}. A resource R is dependent if closing {@code
   * expr} will also close R.
   */
  private static List<Expression> findDependentResources(final Expression expr) {
    final List<Expression> allDependent = new ArrayList<>();

    // immediately generates a JDBC resource e.g. <expr>.prepareStatement()
    final var maybeMCE =
        ASTPatterns.isScopeInMethodCall(expr).filter(JDBCResourceLeakFixer::isJDBCResourceInit);
    if (maybeMCE.isPresent()) {
      allDependent.add(maybeMCE.get());
      allDependent.addAll(findDependentResources(maybeMCE.get()));
      return allDependent;
    }

    // immediately passed as a constructor argument for a closeable resource
    // the wrapping closeable resource is considered dependent
    final var maybeOCE =
        ASTPatterns.isConstructorArgument(expr).filter(JDBCResourceLeakFixer::isCloseableType);
    if (maybeOCE.isPresent()) {
      allDependent.add(maybeOCE.get());
      allDependent.addAll(findDependentResources(maybeOCE.get()));
      return allDependent;
    }

    // is assigned to a local variable and ...
    final var maybeLVD = immediatelyFlowsIntoLocalVariable(expr);
    if (maybeLVD.isPresent()) {
      final var lvd = maybeLVD.get();
      final var scope = lvd.getScope();
      // ... generates JDBC resource or ...
      final Predicate<MethodCallExpr> vdGenerates =
          mce -> isScopeInMethodCall(mce, lvd.getName()) && isJDBCResourceInit(mce);

      scope.stream()
          .flatMap(n -> n.findAll(MethodCallExpr.class, vdGenerates).stream())
          .flatMap(e -> Stream.concat(Stream.of(e), findDependentResources(e).stream()))
          .forEach(allDependent::add);

      // is wrapped by a closeable resource
      final Predicate<ObjectCreationExpr> wrapsVD =
          oce ->
              oce.getArguments().stream()
                  .anyMatch(
                      e ->
                          e.isNameExpr() && e.asNameExpr().getNameAsString().equals(lvd.getName()));
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
  private static boolean immediatelyEscapesMethodScope(final Expression expr) {
    // Returned or argument of a MethodCallExpr
    if (ASTPatterns.isReturnExpr(expr).isPresent()
        || ASTPatterns.isArgumentOfMethodCall(expr).isPresent()) return true;

    // is the init expression of a field
    if (ASTPatterns.isInitExpr(expr).filter(ASTPatterns::isLocalVariableDeclarator).isEmpty())
      return true;

    // Is assigned to a field?
    final var maybeAE = ASTPatterns.isAssigned(expr);
    if (maybeAE.isPresent()) {
      final var ae = maybeAE.get();
      // Currently we have no precise way of knowing if the target is a field, thus
      // if the target is not a ExpressionName of a local variable, we consider it escaping
      final Optional<NameExpr> maybeNameTarget =
          ae.getTarget().isNameExpr() ? Optional.of(ae.getTarget().asNameExpr()) : Optional.empty();
      final var maybeLD =
          maybeNameTarget.flatMap(
              nameExpr ->
                  ASTs.findEarliestLocalDeclarationOf(nameExpr, nameExpr.getNameAsString()));
      return maybeLD.isEmpty();
    }
    return false;
  }

  /** Returns true if {@code vd} is returned or is an argument of a method call. */
  private static boolean escapesRootScope(
      final LocalVariableDeclaration lvd, final Predicate<Node> isInScope) {
    if (!isInScope.test(lvd.getVariableDeclarator())) return true;

    // Returned
    final Predicate<ReturnStmt> isReturned =
        rs ->
            rs.getExpression()
                .filter(
                    e -> e.isNameExpr() && e.asNameExpr().getNameAsString().equals(lvd.getName()))
                .isPresent();
    if (lvd.getScope().stream()
        .anyMatch(n -> n.findFirst(ReturnStmt.class, isReturned).isPresent())) return true;

    // As an argument of a method call
    final Predicate<MethodCallExpr> isCallArgument =
        mce ->
            mce.getArguments().stream()
                .anyMatch(
                    arg ->
                        arg.isNameExpr()
                            && arg.asNameExpr().getNameAsString().equals(lvd.getName()));

    return lvd.getScope().stream()
        .anyMatch(n -> n.findFirst(MethodCallExpr.class, isCallArgument).isPresent());
  }

  /** Returns true if {@code expr} itself escapes or flows into a variable that escapes. */
  private static boolean escapesRootScope(final Expression expr, final Predicate<Node> isInScope) {
    if (immediatelyEscapesMethodScope(expr)) return true;
    // find all the variables it flows into
    final var pair = flowsInto(expr);
    // flows into anything that is not a local variable
    if (!pair.getValue1().isEmpty()) return true;

    final var allVD = pair.getValue0();
    return allVD.stream().anyMatch(vd -> escapesRootScope(vd, isInScope));
  }
}
