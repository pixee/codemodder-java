package io.codemodder.codemods;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import io.codemodder.Either;
import io.codemodder.ast.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A library that contains methods for automatically fixing resource leaks detected by CodeQL's
 * rules "java/database-resource-leak", java/input-resource-leak, and java/output-resource-leak
 * whenever possible.
 */
final class ResourceLeakFixer {

  private static final Logger LOG = LoggerFactory.getLogger(ResourceLeakFixer.class);

  private static final String rootPrefix = "resource";

  /**
   * Detects if an {@link Expression} that creates a resource type is fixable and tries to fix it.
   * Combines {@code isFixable} and {@code tryToFix}.
   */
  public static Optional<TryStmt> checkAndFix(final Expression expr) {
    if (expr instanceof ObjectCreationExpr) {
      // finds the root expression in a chain of new AutoCloseable objects
      ObjectCreationExpr root = findRootExpression(expr.asObjectCreationExpr());
      if (isFixable(root)) {
        return tryToFix(root);
      }
    }
    if (expr instanceof MethodCallExpr) {
      if (isFixable(expr)) {
        return tryToFix(expr.asMethodCallExpr());
      }
    }
    return Optional.empty();
  }

  /**
   * Detects if a {@link Expression} detected by CodeQL that creates a leaking resource is fixable.
   *
   * <p>A resource R is dependent of another resource S if closing S will also close R. The
   * following suffices to check if a resource R can be closed. (*) A resource R can be closed if no
   * dependent resource S exists s.t.: (i) S is not closed within R's scope, and (ii) S escapes R's
   * scope Currently, we cannot test (*), as it requires some dataflow analysis, instead we test for
   * (+): S is assigned to a variable that escapes.
   */
  public static boolean isFixable(final Expression expr) {
    // Can it be wrapped in a try statement?
    if (!isAutoCloseableType(expr)) {
      return false;
    }
    // is it already closed? does it escape?
    // TODO depends on another resource that is already closed?
    if (isClosedOrEscapes(expr)) {
      return false;
    }
    // For ResultSet objects, still need to check if the generating *Statement object does
    // not escape due to the getResultSet() method.
    try {
      if (expr instanceof MethodCallExpr) {
        var mce = expr.asMethodCallExpr();
        if (mce.calculateResolvedType().describe().equals("java.sql.ResultSet")) {
          // should always exist and is a *Statement object
          final var mceScope = mce.getScope().get();
          if (mceScope.isFieldAccessExpr()) {
            return false;
          }
          if (mceScope.isNameExpr()) {
            final var maybeLVD =
                ASTs.findEarliestLocalVariableDeclarationOf(
                    mceScope, mceScope.asNameExpr().getNameAsString());

            if (maybeLVD.filter(lvd -> escapesRootScope(lvd, n -> true)).isPresent()) {
              return false;
            }
          }
        }
      }
    } catch (final UnsolvedSymbolException e) {
      LOG.error("Problem resolving type of : {}", expr, e);
      return false;
    }

    // For any dependent resource s of r if s is not closed and escapes, then r cannot be closed
    final var maybeLVD = immediatelyFlowsIntoLocalVariable(expr);
    if (maybeLVD.isPresent()) {
      final var scope = maybeLVD.get().getScope();
      final Predicate<Node> isInScope = scope::inScope;
      final var allDependent = findDependentResources(expr);
      return allDependent.stream().noneMatch(e -> escapesRootScope(e, isInScope));
    } else {
      final var allDependent = findDependentResources(expr);
      return allDependent.stream().noneMatch(e -> escapesRootScope(e, n -> true));
    }
  }

  private static boolean isClosedOrEscapes(final Expression expr) {
    if (immediatelyEscapesMethodScope(expr)) {
      return true;
    }
    // find all the variables it flows into
    final var allVariables = flowsInto(expr);

    // flows into anything that is not a local variable or parameter
    if (allVariables.stream().anyMatch(Either::isRight)) {
      return true;
    }

    // is any of the assigned variables closed?
    if (allVariables.stream()
        .filter(Either::isLeft)
        .map(Either::getLeft)
        .anyMatch(ld -> !notClosed(ld))) {
      return true;
    }

    // If any of the assigned variables escapes
    return allVariables.stream()
        .filter(Either::isLeft)
        .map(Either::getLeft)
        .anyMatch(ld -> escapesRootScope(ld, x -> true));
  }

  public static ObjectCreationExpr findRootExpression(final ObjectCreationExpr creationExpr) {
    ObjectCreationExpr current = creationExpr;
    var maybeInner = Optional.of(current);
    while (maybeInner.isPresent()) {
      current = maybeInner.get();
      maybeInner =
          ASTs.isArgumentOfObjectCreationExpression(current)
              .filter(ResourceLeakFixer::isAutoCloseableType);
    }
    return current;
  }

  public static Optional<TryStmt> tryToFix(final ObjectCreationExpr creationExpr) {
    final var deque = findInnerExpressions(creationExpr);
    int count = 0;
    final var maybeLVD =
        ASTs.isInitExpr(creationExpr)
            .flatMap(LocalVariableDeclaration::fromVariableDeclarator)
            .map(
                lvd ->
                    lvd instanceof ExpressionStmtVariableDeclaration
                        ? (ExpressionStmtVariableDeclaration) lvd
                        : null)
            .filter(ASTs::isFinalOrNeverAssigned);
    if (maybeLVD.isPresent()) {
      var tryStmt =
          ASTTransforms.wrapIntoResource(
              maybeLVD.get().getStatement(),
              maybeLVD.get().getVariableDeclarationExpr(),
              maybeLVD.get().getScope());
      var cu = creationExpr.findCompilationUnit().get();
      for (var resource : deque) {
        var name = count == 0 ? rootPrefix : rootPrefix + count;
        // TODO try to resolve type, failing to do that use var declaration?
        var type = resource.calculateResolvedType().describe();
        resource.replace(new NameExpr(name));

        var declaration =
            new VariableDeclarator(
                StaticJavaParser.parseType(type.substring(type.lastIndexOf('.') + 1)),
                name,
                resource);
        tryStmt.getResources().addFirst(new VariableDeclarationExpr(declaration));
        ASTTransforms.addImportIfMissing(cu, type);
        count++;
      }
      return Optional.of(tryStmt);
    }
    return Optional.empty();
  }

  /** Tries to fix the leak of {@code expr} and returns its line if it does. */
  public static Optional<TryStmt> tryToFix(final MethodCallExpr mce) {

    // Is LocalDeclarationStmt and Never Assigned -> Wrap as a try resource
    List<Expression> resources = new ArrayList<>();
    Expression root = findRootExpression(mce, resources);
    final var maybeLVD =
        ASTs.isInitExpr(root)
            .flatMap(LocalVariableDeclaration::fromVariableDeclarator)
            .map(
                lvd ->
                    lvd instanceof ExpressionStmtVariableDeclaration
                        ? (ExpressionStmtVariableDeclaration) lvd
                        : null)
            .filter(ASTs::isFinalOrNeverAssigned);
    if (maybeLVD.isPresent()) {
      var lvd = maybeLVD.get();
      // create a new variable for each gathered resource and wrap everything in a try
      var tryStmt =
          ASTTransforms.wrapIntoResource(
              lvd.getStatement(), lvd.getVariableDeclarationExpr(), lvd.getScope());
      // TODO check availability here
      int count = 0;
      var cu = mce.findCompilationUnit().get();
      for (var resource : resources) {
        var name = count == 0 ? rootPrefix : rootPrefix + count;
        // TODO try to resolve type, failing to do that use var declaration?
        var type = resource.calculateResolvedType().describe();

        resource.replace(new NameExpr(name));

        var declaration =
            new VariableDeclarator(
                StaticJavaParser.parseType(type.substring(type.lastIndexOf('.') + 1)),
                name,
                resource);
        tryStmt.getResources().addFirst(new VariableDeclarationExpr(declaration));
        ASTTransforms.addImportIfMissing(cu, type);
        count++;
      }
      return Optional.of(tryStmt);
      // TODO if vde is multiple declarations, extract the relevant vd and wrap it
    }
    // other cases here...
    return Optional.empty();
  }

  private static Deque<Expression> findInnerExpressions(final ObjectCreationExpr creationExpr) {
    var deque = new ArrayDeque<Expression>();
    var maybeExpr =
        creationExpr.getArguments().stream()
            .flatMap(expr -> isAutoCloseableCreation(expr).stream())
            .findFirst();
    while (maybeExpr.isPresent()) {
      deque.addLast(maybeExpr.get());
      maybeExpr =
          maybeExpr.get().getArguments().stream()
              .flatMap(expr -> isAutoCloseableCreation(expr).stream())
              .findFirst();
    }
    return deque;
  }

  private static Expression findRootExpression(
      final Expression expr, final List<Expression> resources) {
    // if a resource can be closed, so can all its dependent resources

    // e.g <expr>.executeQuery(query);
    var maybeCall = ASTs.isScopeInMethodCall(expr).filter(ResourceLeakFixer::isJDBCResourceInit);
    if (maybeCall.isPresent()) {
      resources.add(expr);
      return findRootExpression(maybeCall.get(), resources);
    }
    return expr;
  }

  /**
   * Checks if {@code expr} is immediately assigned to a local variable {@code v} through an
   * assignment or initializer.
   */
  private static Optional<LocalVariableDeclaration> immediatelyFlowsIntoLocalVariable(
      final Expression expr) {
    final var maybeInit = ASTs.isInitExpr(expr).filter(ASTs::isLocalVariableDeclarator);
    if (maybeInit.isPresent()) {
      return maybeInit.flatMap(LocalVariableDeclaration::fromVariableDeclarator);
    }
    return ASTs.isAssigned(expr)
        .map(ae -> ae.getTarget().isNameExpr() ? ae.getTarget().asNameExpr() : null)
        .flatMap(ne -> ASTs.findEarliestLocalVariableDeclarationOf(ne, ne.getNameAsString()));
  }

  /** Checks if the expression implements the {@link java.lang.AutoCloseable} interface. */
  private static boolean isAutoCloseableType(final Expression expr) {
    try {
      return expr.calculateResolvedType().isReferenceType()
          && expr.calculateResolvedType().asReferenceType().getAllAncestors().stream()
              .anyMatch(t -> t.describe().equals("java.lang.AutoCloseable"));
    } catch (RuntimeException e) {
      LOG.error("Problem resolving type of : {}", expr);
      return false;
    }
  }

  /** Checks if the expression creates a {@link java.lang.AutoCloseable} */
  private static Optional<ObjectCreationExpr> isAutoCloseableCreation(final Expression expr) {
    return Optional.of(expr)
        .filter(Expression::isObjectCreationExpr)
        .map(Expression::asObjectCreationExpr)
        .filter(oce -> isAutoCloseableType(expr));
  }

  /** Checks if the expression creates a {@link java.lang.AutoCloseable} */
  private static boolean isResourceInit(final Expression expr) {
    return (expr.isMethodCallExpr()
            && (isJDBCResourceInit(expr.asMethodCallExpr())
                || isFilesResourceInit(expr.asMethodCallExpr())))
        || isAutoCloseableCreation(expr).isPresent();
  }

  private static Either<LocalDeclaration, Node> isLocalDeclaration(Node n) {
    if (n instanceof VariableDeclarator) {
      var maybe = LocalVariableDeclaration.fromVariableDeclarator((VariableDeclarator) n);
      return maybe
          .<Either<LocalDeclaration, Node>>map(Either::left)
          .orElseGet(() -> Either.right(n));
    }
    if (n instanceof Parameter) {
      return Either.left(new ParameterDeclaration((Parameter) n));
    }
    return Either.right(n);
  }

  /** Checks if {@code expr} creates an AutoCloseable Resource. */
  private static boolean isFilesResourceInit(final MethodCallExpr expr) {
    try {
      var hasFilesScope =
          expr.getScope()
              .map(mce -> mce.calculateResolvedType().describe())
              .filter("java.nio.file.Files"::equals);
      return hasFilesScope.isPresent() && expr.getNameAsString().startsWith("new");
    } catch (final UnsolvedSymbolException e) {
      LOG.error("Problem resolving type of : {}", expr, e);
    }
    return false;
  }

  /** Checks if {@code expr} creates a JDBC Resource. */
  private static boolean isJDBCResourceInit(final MethodCallExpr expr) {
    final Predicate<MethodCallExpr> isResultSetGen =
        mce ->
            switch (mce.getNameAsString()) {
              case "executeQuery", "getResultSet", "getGeneratedKeys" -> true;
              default -> false;
            };
    final Predicate<MethodCallExpr> isStatementGen =
        mce ->
            switch (mce.getNameAsString()) {
              case "createStatement", "prepareCall", "prepareStatement" -> true;
              default -> false;
            };
    final Predicate<MethodCallExpr> isReaderGen =
        mce ->
            switch (mce.getNameAsString()) {
              case "getCharacterStream", "getNCharacterStream" -> true;
              default -> false;
            };
    final Predicate<MethodCallExpr> isDependent = isResultSetGen.or(isStatementGen.or(isReaderGen));
    return isDependent.test(expr);
  }

  /**
   * Finds a superset of all the local variables that {@code expr} will be assigned to. The search
   * works recursively, meaning if, for example, {@code b = expr; a = b;}, {@code a} will be on the
   * list.
   *
   * @return A list where each element is either a {@link LocalDeclaration} that {@code expr} will
   *     eventually reach, or a {@link Node} that {@code expr} was assigned/initialized into.
   */
  private static List<Either<LocalDeclaration, Node>> flowsInto(final Expression expr) {
    // is immediately assigned as an init expr
    Optional<Either<LocalDeclaration, Node>> maybeExpr =
        ASTs.isInitExpr(expr)
            .flatMap(LocalVariableDeclaration::fromVariableDeclarator)
            .map(Either::left);

    // is immediately assigned
    maybeExpr =
        maybeExpr.or(
            () ->
                ASTs.isAssigned(expr)
                    .filter(ae -> ae.getTarget().isNameExpr())
                    .map(ae -> ae.getTarget().asNameExpr())
                    .flatMap(ne -> ASTs.findNonCallableSimpleNameSource(ne.getName()))
                    .map(ResourceLeakFixer::isLocalDeclaration));
    return maybeExpr
        .map(e -> e.ifLeftOrElseGet(ResourceLeakFixer::flowsInto, n -> List.of(e)))
        .orElse(List.of());
  }

  public static List<Either<LocalDeclaration, Node>> flowsInto(final LocalDeclaration ld) {
    return flowsIntoImpl(ld, new HashSet<>());
  }

  private static List<Either<LocalDeclaration, Node>> flowsIntoImpl(
      final LocalDeclaration ld, final HashSet<Node> memory) {
    if (memory.contains(ld.getDeclaration())) return List.of();
    else memory.add(ld.getDeclaration());

    final Predicate<AssignExpr> isRHSOfAE =
        ae ->
            ae.getValue().isNameExpr()
                && ASTs.findNonCallableSimpleNameSource(ae.getValue().asNameExpr().getName())
                    .filter(n -> n == ld.getDeclaration())
                    .isPresent();

    // assignments i.e. a = v, where v is ld's name
    var allAETarget =
        ld.getScope().stream()
            .flatMap(n -> n.findAll(AssignExpr.class, isRHSOfAE).stream())
            .map(AssignExpr::getTarget);

    // filter assignments like v = v;
    allAETarget =
        allAETarget.filter(
            e -> !(e.isNameExpr() && e.asNameExpr().getNameAsString().equals(ld.getName())));

    // recursively flowsInto any assignment if the lhs is a name of a local declaration
    final Stream<Either<LocalDeclaration, Node>> fromAssignments =
        allAETarget.flatMap(
            e ->
                e.isNameExpr()
                    ? ASTs.findEarliestLocalDeclarationOf(e.asNameExpr().getName())
                        .map(decl -> flowsIntoImpl(decl, memory).stream())
                        .orElse(Stream.of(Either.right(e)))
                    : Stream.of(Either.right(e)));

    // Checks if the init expression is v
    final Predicate<VariableDeclarator> isRHSOfVD =
        varDecl ->
            varDecl
                .getInitializer()
                .filter(
                    init ->
                        init.isNameExpr()
                            && ASTs.findNonCallableSimpleNameSource(init.asNameExpr().getName())
                                .filter(n -> n == ld.getDeclaration())
                                .isPresent())
                .isPresent();

    // gather all local variable declarations with v as an init expression
    var allLVD =
        ld.getScope().stream()
            .flatMap(n -> n.findAll(VariableDeclarator.class, isRHSOfVD).stream())
            .flatMap(vd -> LocalVariableDeclaration.fromVariableDeclarator(vd).stream());

    // recursively flowsInto
    final Stream<Either<LocalDeclaration, Node>> fromInit =
        allLVD.flatMap(lvd -> flowsIntoImpl(lvd, memory).stream());

    return Stream.concat(
            Stream.of(Either.<LocalDeclaration, Node>left(ld)),
            Stream.concat(fromAssignments, fromInit))
        .toList();
  }

  private static List<Expression> findDirectlyDependentResources(final LocalDeclaration ld) {
    var jdbcResources = ld.findAllMethodCalls().filter(ResourceLeakFixer::isJDBCResourceInit);

    // Checks if the object creation has ld as an argument
    // TODO check if any resources can have more than one argument
    Predicate<ObjectCreationExpr> wrapsLD =
        oce ->
            oce.getArguments()
                .getFirst()
                .filter(arg -> arg.isNameExpr() && ld.isReference(arg.asNameExpr()))
                .isPresent();
    var oceResources =
        ld.getScope().stream()
            .flatMap(n -> n.findAll(ObjectCreationExpr.class).stream())
            .filter(wrapsLD);

    return Stream.concat(jdbcResources, oceResources).collect(Collectors.toList());
  }

  public static Optional<Expression> findResourceInit(final NameExpr name) {
    var maybeResourceInit =
        ASTs.findEarliestLocalVariableDeclarationOf(name, name.getNameAsString())
            .filter(ASTs::isFinalOrNeverAssigned)
            .flatMap(lvd -> lvd.getVariableDeclarator().getInitializer());
    if (maybeResourceInit.isPresent() && maybeResourceInit.get() instanceof NameExpr ne) {
      return findResourceInit(ne);
    } else {
      return maybeResourceInit.filter(ResourceLeakFixer::isResourceInit);
    }
  }

  /**
   * Find all the directly dependent resources of {@code expr}. A resource R is dependent if closing
   * {@code expr} will also close R.
   */
  private static List<Expression> findDirectlyDependentResources(final Expression expr) {
    final List<Expression> allDependent = new ArrayList<>();

    // immediately generates a JDBC resource e.g. <expr>.prepareStatement()
    final var maybeMCE =
        ASTs.isScopeInMethodCall(expr).filter(ResourceLeakFixer::isJDBCResourceInit);
    if (maybeMCE.isPresent()) {
      allDependent.add(maybeMCE.get());
      return allDependent;
    }

    // e.g. <expr> = new BufferedReader(<newExpr>)
    // newExpr is considered dependent
    if (expr instanceof ObjectCreationExpr) {
      var maybeArg =
          expr.asObjectCreationExpr().getArguments().stream()
              .filter(ResourceLeakFixer::isAutoCloseableType)
              .findFirst();
      // try to find the source of a NameExpr
      // TODO It may be the case that NameExpr references a parameter here, not supported currently
      var maybeResourceInit =
          maybeArg.filter(Expression::isNameExpr).flatMap(e -> findResourceInit(e.asNameExpr()));
      if (maybeResourceInit.isPresent()) {
        maybeResourceInit.ifPresent(allDependent::add);
      } else {
        maybeArg.ifPresent(allDependent::add);
      }
    }

    // immediately passed as a constructor argument for a closeable resource
    // while not tecnically dependent, closing expr will make the new resouce useless
    // e.g. var br = new BufferedReader(<expr>)
    final var maybeOCE =
        ASTs.isConstructorArgument(expr).filter(ResourceLeakFixer::isAutoCloseableType);
    if (maybeOCE.isPresent()) {
      allDependent.add(maybeOCE.get());
      return allDependent;
    }

    var allFlown = flowsInto(expr).stream().filter(Either::isLeft).map(Either::getLeft);

    allFlown.flatMap(ld -> findDirectlyDependentResources(ld).stream()).forEach(allDependent::add);

    return allDependent;
  }

  /** Finds all the dependent resource recursively. */
  private static List<Expression> findDependentResources(final Expression expr) {
    HashSet<Node> memory = new HashSet<>();
    return findDependentResourcesImpl(expr, memory);
  }

  /** Finds all the dependent resource recursively. */
  private static List<Expression> findDependentResourcesImpl(
      final Expression expr, HashSet<Node> memory) {
    if (memory.contains(expr)) {
      return List.of();
    }
    memory.add(expr);
    return findDirectlyDependentResources(expr).stream()
        .filter(res -> !memory.contains(res))
        .flatMap(
            res -> Stream.concat(Stream.of(res), findDependentResourcesImpl(res, memory).stream()))
        .toList();
  }

  /**
   * Checks if an object created/accessed by {@code expr} escapes the scope of its encompassing
   * method immediately, that is, without being assigned. It escapes if it is assigned to a field,
   * returned, or is the argument of a method call.
   */
  private static boolean immediatelyEscapesMethodScope(final Expression expr) {
    // anything that is not a resource creation escapes
    // e.g. field access, nameexpr of parameter, method calls, etc.
    if (!isResourceInit(expr)) {
      return true;
    }

    // Returned or argument of a MethodCallExpr
    if (ASTs.isReturnExpr(expr).isPresent() || ASTs.isArgumentOfMethodCall(expr).isPresent()) {
      return true;
    }

    // is the init expression of a field
    return ASTs.isInitExpr(expr).flatMap(ASTs::isVariableOfField).isPresent();
  }

  /**
   * Returns true if a {@link LocalDeclaration ld} of a resource is not closed. It may return true
   * if {@code ld} is closed.
   */
  private static boolean notClosed(final LocalDeclaration ld) {
    // if close is never called
    return ld.findAllMethodCalls().noneMatch(mce -> mce.getNameAsString().equals("close"))
        &&
        // is not a try resource
        ld instanceof LocalVariableDeclaration
        && ASTs.isResource(((LocalVariableDeclaration) ld).getVariableDeclarator()).isEmpty();
  }

  /** Returns true if {@code ld} is returned or is an argument of a method call. */
  private static boolean escapesRootScope(
      final LocalDeclaration ld, final Predicate<Node> isInScope) {
    if (!isInScope.test(ld.getDeclaration())) return true;
    return ld.findAllReferences()
        .anyMatch(
            ne -> ASTs.isReturnExpr(ne).isPresent() || ASTs.isArgumentOfMethodCall(ne).isPresent());
  }

  private static boolean escapesRootScope(final Expression expr, final Predicate<Node> isInScope) {
    if (immediatelyEscapesMethodScope(expr)) return true;
    // find all the variables it flows into
    final var allVariables = flowsInto(expr);

    // flows into anything that is not a local variable or parameter
    if (allVariables.stream().anyMatch(Either::isRight)) {
      return true;
    }

    // If any of the assigned variables is not closed and escapes
    return allVariables.stream()
        .filter(Either::isLeft)
        .map(Either::getLeft)
        .anyMatch(ld -> notClosed(ld) && escapesRootScope(ld, isInScope));
  }
}
