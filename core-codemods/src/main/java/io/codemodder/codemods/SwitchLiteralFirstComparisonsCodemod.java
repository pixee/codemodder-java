package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import io.codemodder.*;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sarif.pmd.PmdScan;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

/**
 * A codemod for automatically switching the order of literals and variables in comparisons so
 * they're guaranteed not to throw {@link NullPointerException} when the variable is unexpectedly
 * null.
 */
@Codemod(
    id = "pixee:java/switch-literal-first",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class SwitchLiteralFirstComparisonsCodemod
    extends SarifPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public SwitchLiteralFirstComparisonsCodemod(
      @PmdScan(ruleId = "category/java/bestpractices.xml/LiteralsFirstInComparisons")
          final RuleSarif ruleSarif) {
    super(ruleSarif, MethodCallExpr.class);
  }

  @Override
  public ChangesResult onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    // some of the methods that this rule applies to are not flippable, like compareTo() would
    // change the logic after
    if (!flippableComparisonMethods.contains(methodCallExpr.getNameAsString())) {
      return ChangesResult.noChanges;
    }

    final List<VariableDeclarator> variableDeclarators = cu.findAll(VariableDeclarator.class);

    final Optional<SimpleName> simpleNameOptional = getSimpleNameFromMethodCallExpr(methodCallExpr);

    /**
     * This codemod will not be executed if:
     *
     * <ol>
     *   <li>Variable has a previous not null assertion
     *   <li>Variable has a {@link @NotNull} or {@link @Nonnull} annotation
     *   <li>Variable was previously initialized to a not null value
     * </ol>
     */
    if (simpleNameOptional.isPresent()
        && (hasSimpleNameNotNullAnnotation(cu, simpleNameOptional.get(), variableDeclarators)
            || hasSimpleNamePreviousNullAssertion(cu, simpleNameOptional.get())
            || isSimpleNameANotNullInitializedVariableDeclarator(cu, simpleNameOptional.get()))) {
      return ChangesResult.noChanges;
    }

    Expression leftSide = methodCallExpr.getScope().get();
    Expression rightSide = methodCallExpr.getArgument(0);
    try {
      final ResolvedType leftType = leftSide.calculateResolvedType();
      if ("Ljava/lang/String;".equals(leftType.toDescriptor())) {
        methodCallExpr.setScope(rightSide);
        methodCallExpr.setArgument(0, leftSide);
        return ChangesResult.changesApplied;
      }
    } catch (final UnsolvedSymbolException e) {
      // expected in cases where we can't resolve the type
    }

    return ChangesResult.noChanges;
  }

  /**
   * Method used to check if variable (nameNode) has a previous node that represents:
   *
   * <ol>
   *   <li>A not null assertion
   *   <li>A not null annotation
   *   <li>An initialization to a not null value
   * </ol>
   */
  private boolean isPreviousNodeBefore(final Node nameNode, final Node previousNode) {
    final Optional<Range> nameNodeRange = nameNode.getRange();
    final Optional<Range> previousNodeRange = previousNode.getRange();
    if (nameNodeRange.isEmpty() || previousNodeRange.isEmpty()) {
      return false;
    }
    return previousNodeRange.get().begin.isBefore(nameNodeRange.get().begin);
  }

  /**
   * Checks if the given Variable {@link SimpleName} has a previous null assertion in the {@link
   * CompilationUnit}.
   */
  private boolean hasSimpleNamePreviousNullAssertion(
      final CompilationUnit cu, final SimpleName name) {
    final List<NullLiteralExpr> nullLiterals = cu.findAll(NullLiteralExpr.class);

    if (nullLiterals != null && !nullLiterals.isEmpty()) {
      return nullLiterals.stream()
          .filter(
              nullLiteralExpr ->
                  nullLiteralExpr.getParentNode().isPresent()
                      && nullLiteralExpr.getParentNode().get() instanceof BinaryExpr)
          .map(nullLiteralExpr -> (BinaryExpr) nullLiteralExpr.getParentNode().get())
          .filter(
              parentBinaryExpr -> parentBinaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS)
          .anyMatch(
              parentBinaryExpr -> {
                final Node left = parentBinaryExpr.getLeft();
                final Node right = parentBinaryExpr.getRight();
                return isBinaryNodeChildPreviouslyCreatedAndEqualToSimpleName(left, name)
                    || isBinaryNodeChildPreviouslyCreatedAndEqualToSimpleName(right, name);
              });
    }

    return false;
  }

  /**
   * Checks if the given {@link Node} is a binary expression child node and is equal to the provided
   * {@link SimpleName}. Additionally, it verifies if the child node is located before the provided
   * {@link SimpleName}.
   */
  private boolean isBinaryNodeChildPreviouslyCreatedAndEqualToSimpleName(
      final Node child, final SimpleName name) {
    return child instanceof NameExpr nameExpr
        && nameExpr.getName().equals(name)
        && isPreviousNodeBefore(name, nameExpr.getName());
  }

  /**
   * Checks if the given {@link SimpleName} variable has a @NotNull or @Nonnull annotation in the
   * provided {@link CompilationUnit}.
   */
  private boolean hasSimpleNameNotNullAnnotation(
      final CompilationUnit cu,
      final SimpleName simpleName,
      final List<VariableDeclarator> variableDeclarators) {
    final List<FieldDeclaration> fieldDeclarations = cu.findAll(FieldDeclaration.class);
    final List<Parameter> parameters = cu.findAll(Parameter.class);

    final List<Node> annotationNodesCandidates = new ArrayList<>();
    annotationNodesCandidates.addAll(variableDeclarators);
    annotationNodesCandidates.addAll(fieldDeclarations);
    annotationNodesCandidates.addAll(parameters);

    final List<Node> annotations = filterNodesWithNotNullAnnotations(annotationNodesCandidates);

    return annotations.stream()
        .anyMatch(
            annotation ->
                isSimpleNotNullAnnotationForParameterOrVariable(annotation, simpleName)
                    || isSimpleNotNullAnnotationForFieldDeclaration(annotation, simpleName));
  }

  /**
   * Checks if the given {@link Node} represents a @NotNull or @Nonnull annotation for a
   * FieldDeclaration and the associated VariableDeclarator has the provided {@link SimpleName}.
   */
  private boolean isSimpleNotNullAnnotationForFieldDeclaration(
      final Node annotation, final SimpleName simpleName) {
    if (annotation instanceof FieldDeclaration fieldDeclaration) {
      final List<VariableDeclarator> fieldDeclarationVariables = fieldDeclaration.getVariables();
      return fieldDeclarationVariables.stream()
          .anyMatch(
              variableDeclarator ->
                  variableDeclarator.getName().equals(simpleName)
                      && isPreviousNodeBefore(simpleName, variableDeclarator.getName()));
    }
    return false;
  }

  /**
   * Checks if the given {@link Node} represents a @NotNull or @Nonnull annotation for a Parameter
   * or VariableDeclarator, and the associated SimpleName matches the provided {@link SimpleName}.
   */
  private boolean isSimpleNotNullAnnotationForParameterOrVariable(
      final Node annotation, final SimpleName simpleName) {
    if (annotation instanceof Parameter || annotation instanceof VariableDeclarator) {
      final SimpleName annotationSimpleName = ((NodeWithSimpleName<?>) annotation).getName();
      return annotationSimpleName.equals(simpleName)
          && isPreviousNodeBefore(simpleName, annotationSimpleName);
    }

    return false;
  }

  /**
   * Filters the provided list of nodes to include only those with @NotNull or @Nonnull annotations.
   */
  private List<Node> filterNodesWithNotNullAnnotations(final List<Node> annotationNodes) {
    return annotationNodes.stream()
        .filter(node -> node instanceof NodeWithAnnotations<?>)
        .filter(node -> !((NodeWithAnnotations<?>) node).getAnnotations().isEmpty())
        .filter(
            node -> hasNotNullOrNonnullAnnotation(((NodeWithAnnotations<?>) node).getAnnotations()))
        .toList();
  }

  /** Checks if the provided list of annotations contains any @NotNull or @Nonnull annotations. */
  private boolean hasNotNullOrNonnullAnnotation(final NodeList<AnnotationExpr> annotations) {
    return annotations.stream().anyMatch(this::isNotNullOrNonnullAnnotation);
  }

  private boolean isNotNullOrNonnullAnnotation(final AnnotationExpr annotation) {
    final Name annotationName = annotation.getName();
    final String name = annotationName.getIdentifier();
    return "NotNull".equals(name) || "Nonnull".equals(name);
  }

  /**
   * Checks if the provided {@link SimpleName} variable corresponds to a {@link VariableDeclarator}
   * that was previously initialized to a non-null expression.
   */
  private boolean isSimpleNameANotNullInitializedVariableDeclarator(
      final CompilationUnit cu, final SimpleName targetName) {

    final Optional<VariableDeclarator> variableDeclaratorOptional =
        getDeclaredVariable(cu, targetName);

    if (variableDeclaratorOptional.isEmpty()
        || variableDeclaratorOptional.get().getInitializer().isEmpty()) {
      return false;
    }

    return isNullSafeExpression(cu, variableDeclaratorOptional.get().getInitializer().get());
  }

  /**
   * Retrieves the {@link SimpleName} from the given {@link MethodCallExpr}. This codemod assumes
   * that {@link MethodCallExpr} nodes contain only one {@link SimpleName} node.
   */
  private Optional<SimpleName> getSimpleNameFromMethodCallExpr(
      final MethodCallExpr methodCallExpr) {
    final List<SimpleName> simpleNames =
        methodCallExpr.getChildNodes().stream()
            .filter(NameExpr.class::isInstance)
            .map(node -> ((NameExpr) node).getName())
            .toList();
    return simpleNames.isEmpty() ? Optional.empty() : Optional.of(simpleNames.get(0));
  }

  private boolean isNullSafeExpression(final CompilationUnit cu, final Expression expression) {
    if (expression instanceof NullLiteralExpr) {
      return false;
    }

    if (expression instanceof MethodCallExpr methodCallExpr) {
      return isNullSafeMethodExpr(cu, methodCallExpr);
    }

    return true;
  }

  private boolean isNullSafeMethodExpr(
      final CompilationUnit cu, final MethodCallExpr methodCallExpr) {
    final Optional<Expression> optionalScope = methodCallExpr.getScope();

    final String method = methodCallExpr.getName().getIdentifier();

    // Static import case for example: import static
    // org.apache.commons.lang3.StringUtils.defaultString
    if (optionalScope.isEmpty()) {
      return isNullSafeImportLibrary(cu, methodCallExpr.getName().getIdentifier(), method);
    }

    final Expression scope = optionalScope.get();

    // Using java.lang.String's method
    if (scope instanceof StringLiteralExpr) {
      return commonMethodsThatCantReturnNull.contains("java.lang.String#".concat(method));
    }

    // Using full import name as scope of method, for example
    // String str = org.apache.commons.lang3.StringUtils.defaultString("")
    if (scope instanceof FieldAccessExpr fieldAccessExpr) {
      final String fullImportName = fieldAccessExpr.toString();
      return commonMethodsThatCantReturnNull.contains(fullImportName.concat("#").concat(method));
    }

    if (scope instanceof NameExpr scopeName) {

      if (!isVariable(cu, scopeName)) {
        // check if scope is non-static import like: import org.apache.commons.lang3.StringUtils
        return isNullSafeImportLibrary(cu, scopeName.getName().getIdentifier(), method);
      }

      final Optional<VariableDeclarator> variableDeclaratorOptional =
          getDeclaredVariable(cu, scopeName.getName());

      if (variableDeclaratorOptional.isEmpty()) {
        return false;
      }

      final String type = variableDeclaratorOptional.get().getTypeAsString();

      // when scope is an object variable, check class type to determine if it is an implicit or
      // explicit import
      return isClassObjectMethodNullSafe(cu, type, method);
    }

    return false;
  }

  /** Some basic java lang type classes */
  private boolean isClassObjectMethodNullSafe(
      final CompilationUnit cu, final String type, final String method) {
    switch (type) {
      case "String" -> {
        return commonMethodsThatCantReturnNull.contains("java.lang.String#".concat(method));
      }
      case "Integer" -> {
        return commonMethodsThatCantReturnNull.contains("java.lang.Integer#".concat(method));
      }
      case "Double" -> {
        return commonMethodsThatCantReturnNull.contains("java.lang.Double#".concat(method));
      }
      case "Character" -> {
        return commonMethodsThatCantReturnNull.contains("java.lang.Character#".concat(method));
      }
      case "Long" -> {
        return commonMethodsThatCantReturnNull.contains("java.lang.Long#".concat(method));
      }
      default -> {
        return isNullSafeImportLibrary(cu, type, method);
      }
    }
  }

  private boolean isVariable(final CompilationUnit cu, final NameExpr nameExpr) {
    final SimpleName simpleName = nameExpr.getName();
    final Optional<VariableDeclarator> variableDeclaratorOptional =
        getDeclaredVariable(cu, simpleName);
    return variableDeclaratorOptional.isPresent();
  }

  private boolean isNullSafeImportLibrary(
      final CompilationUnit cu, final String identifier, final String method) {
    final Optional<ImportDeclaration> optionalImport =
        cu.getImports().stream()
            .filter(importName -> importName.getName().getIdentifier().equals(identifier))
            .findFirst();

    if (optionalImport.isEmpty()) {
      return false;
    }

    if (optionalImport.get().isStatic()
        && optionalImport.get().getName().getQualifier().isEmpty()) {
      return false;
    }

    final Name importDeclaration =
        optionalImport.get().isStatic()
            ? optionalImport.get().getName().getQualifier().get()
            : optionalImport.get().getName();

    return commonMethodsThatCantReturnNull.contains(
        importDeclaration.asString().concat("#").concat(method));
  }

  private Optional<VariableDeclarator> getDeclaredVariable(
      final CompilationUnit cu, final SimpleName simpleName) {
    final List<VariableDeclarator> variableDeclarators = cu.findAll(VariableDeclarator.class);
    return variableDeclarators.stream()
        .filter(declarator -> declarator.getName().equals(simpleName))
        .filter(declarator -> isPreviousNodeBefore(simpleName, declarator.getName()))
        .findFirst();
  }

  private static final Set<String> flippableComparisonMethods =
      Set.of("equals", "equalsIgnoreCase");

  private static final String TEST_IMPLICIT_IMPORT = "io.codemodder.codemods.Codemod#printMessage";
  private static final List<String> commonMethodsThatCantReturnNull =
      List.of(
          TEST_IMPLICIT_IMPORT,
          "java.lang.String#replace",
          "org.apache.commons.lang3.StringUtils#defaultString");
}
