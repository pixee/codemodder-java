package io.codemodder.codemods;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import io.codemodder.Either;
import io.codemodder.ast.*;
import java.util.ArrayList;
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
  public static Optional<Integer> checkAndFix(final Expression expr) {
    return Optional.empty();
  }

  /** Checks if the created object implements the {@link java.io.AutoCloseable} interface. */
  private static boolean isAutoCloseableType(final Expression expr) {
    return expr.calculateResolvedType().isReferenceType()
        && expr.calculateResolvedType().asReferenceType().getAllAncestors().stream()
            .anyMatch(t -> t.describe().equals("java.io.AutoCloseable"));
  }

  private static Either<LocalDeclaration, Node> isLocalDeclaration(Node n) {
    if (n instanceof VariableDeclarator) {
      var maybe =
          LocalVariableDeclaration.fromVariableDeclarator((VariableDeclarator) n).map(lvd -> lvd);
      if (maybe.isPresent()) {
        return Either.left(maybe.get());
      } else {
        return Either.right(n);
      }
    }
    if (n instanceof Parameter) {
      return Either.left(new ParameterDeclaration((Parameter) n));
    }
    return Either.right(n);
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
   * Finds a superset of all the local variables that {@code expr} will be assigned to. The search
   * works recursively, meaning if, for example, {@code b = expr; a = b;}, {@code a} will be on the
   * list.
   *
   * @return A list where each element is either a {@link LocalDeclaration} that {@code expr} will
   *     eventually reach, or a {@link Node} that {@code expr} was assigned/initialized into.
   */
  private static List<Either<LocalDeclaration, Node>> flowsInto(final Expression expr) {
    // is immediately assigned as an init expr
    final Optional<Either<LocalDeclaration, Node>> maybeExpr =
        ASTs.isInitExpr(expr)
            .flatMap(vd -> LocalVariableDeclaration.fromVariableDeclarator(vd))
            .map(lvd -> Either.left(lvd));

    // is immediately assigned
    maybeExpr.or(
        () ->
            ASTs.isAssigned(expr)
                .filter(ae -> ae.getTarget().isNameExpr())
                .map(ae -> ae.getTarget().asNameExpr())
                .flatMap(ne -> ASTs.findNonCallableSimpleNameSource(ne.getName()))
                .map(ResourceLeakFixer::isLocalDeclaration));
    return maybeExpr
        .map(e -> e.ifLeftOrElseGet(ld -> flowsInto(ld), n -> List.of(e)))
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
            .map(ae -> ae.getTarget());

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

    // gather all local variable declarations with v as a init expression
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
        .collect(Collectors.toList());
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
      allDependent.addAll(findDirectlyDependentResources(maybeMCE.get()));
      return allDependent;
    }

    // immediately passed as a constructor argument for a closeable resource
    // the wrapping closeable resource is considered dependent
    final var maybeOCE =
        ASTs.isConstructorArgument(expr).filter(ResourceLeakFixer::isAutoCloseableType);
    if (maybeOCE.isPresent()) {
      allDependent.add(maybeOCE.get());
      allDependent.addAll(findDirectlyDependentResources(maybeOCE.get()));
      return allDependent;
    }

    var allFlown = flowsInto(expr).stream().filter(e -> e.isLeft()).map(e -> e.getLeft());

    allFlown.flatMap(ld -> findDirectlyDependentResources(ld).stream()).forEach(allDependent::add);

    return allDependent;
  }

  /** Finds all the dependent resource recursively. */
  private static List<Expression> findDependentResources(final Expression expr) {
    return findDirectlyDependentResources(expr).stream()
        .flatMap(res -> Stream.concat(Stream.of(res), findDependentResources(res).stream()))
        .collect(Collectors.toList());
  }
}
