package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import com.github.javaparser.resolution.types.ResolvedType;
import io.codemodder.*;
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
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Result result) {
    // some of the methods that this rule applies to are not flippable, like compareTo() would
    // change the logic after
    if (!flippableComparisonMethods.contains(methodCallExpr.getNameAsString())) {
      return false;
    }

    final List<VariableDeclarator> variableDeclarators = cu.findAll(VariableDeclarator.class);

    final SimpleName simpleName = getSimpleNameFromMethodCallExpr(methodCallExpr);

    /**
     * This codemod will not be executed if: 1. Variable was previously initialized to a not null
     * value 2. Variable has a previous not null assertion 3. Variable has a {@code @NotNull} or
     * {@code @Nonnull} annotation
     */
    if (isSimpleNameANotNullInitializedVariableDeclarator(variableDeclarators, simpleName)
        || hasSimpleNameNotNullAnnotation(cu, simpleName, variableDeclarators)
        || hasSimpleNamePreviousNullAssertion(cu, simpleName)) {
      return false;
    }

    Expression leftSide = methodCallExpr.getScope().get();
    Expression rightSide = methodCallExpr.getArgument(0);
    try {
      final ResolvedType leftType = leftSide.calculateResolvedType();
      if ("Ljava/lang/String;".equals(leftType.toDescriptor())) {
        methodCallExpr.setScope(rightSide);
        methodCallExpr.setArgument(0, leftSide);
        return true;
      }
    } catch (final UnsolvedSymbolException e) {
      // expected in cases where we can't resolve the type
    }

    return false;
  }

  /**
   * Method used to check if variable (nameNode) has a previous node that represents: 1. A not null
   * assertion 2. A not null annotation 3. An initialization to a not null value
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
   * Checks if the given Variable {@code SimpleName} has a previous null assertion in the {@code
   * CompilationUnit}.
   */
  private boolean hasSimpleNamePreviousNullAssertion(
      final CompilationUnit cu, final SimpleName name) {
    final List<NullLiteralExpr> nullLiterals = cu.findAll(NullLiteralExpr.class);

    if (nullLiterals != null && !nullLiterals.isEmpty()) {
      for (final NullLiteralExpr nullLiteralExpr : nullLiterals) {
        if (nullLiteralExpr.getParentNode().isPresent()
            && nullLiteralExpr.getParentNode().get() instanceof BinaryExpr parentBinaryExpr
            && parentBinaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS) {
          final Node left = parentBinaryExpr.getLeft();
          final Node right = parentBinaryExpr.getRight();
          if (isBinaryNodeChildEqualToSimpleName(left, name)
              || isBinaryNodeChildEqualToSimpleName(right, name)) {
            return true;
          }
        }
      }
    }

    return false;
  }

  /**
   * Checks if the given {@code Node} is a binary expression child node and is equal to the provided
   * {@code SimpleName}. Additionally, it verifies if the child node is located before the provided
   * {@code SimpleName}.
   */
  private boolean isBinaryNodeChildEqualToSimpleName(final Node child, final SimpleName name) {
    return child instanceof NameExpr nameExpr
        && nameExpr.getName().equals(name)
        && isPreviousNodeBefore(name, nameExpr.getName());
  }

  /**
   * Checks if the given {@code SimpleName} variable has a @NotNull or @Nonnull annotation in the
   * provided {@code CompilationUnit}.
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

    if (!annotations.isEmpty()) {
      for (final Node annotation : annotations) {

        if (isSimpleNotNullAnnotationForParameterOrVariable(annotation, simpleName)
            || isSimpleNotNullAnnotationForFieldDeclaration(annotation, simpleName)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Checks if the given {@code Node} represents a @NotNull or @Nonnull annotation for a
   * FieldDeclaration and the associated VariableDeclarator has the provided {@code SimpleName}.
   */
  private boolean isSimpleNotNullAnnotationForFieldDeclaration(
      final Node annotation, final SimpleName simpleName) {
    if (annotation instanceof FieldDeclaration fieldDeclaration) {
      final List<VariableDeclarator> fieldDeclarationVariables = fieldDeclaration.getVariables();
      for (final VariableDeclarator variableDeclarator : fieldDeclarationVariables) {
        final SimpleName variableSimpleName = variableDeclarator.getName();
        if (variableSimpleName.equals(simpleName)
            && isPreviousNodeBefore(simpleName, variableSimpleName)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Checks if the given {@code Node} represents a @NotNull or @Nonnull annotation for a Parameter
   * or VariableDeclarator, and the associated SimpleName matches the provided {@code SimpleName}.
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
    final List<Node> nodesWithNotNullAnnotations = new ArrayList<>();
    for (final Node node : annotationNodes) {
      if (node instanceof NodeWithAnnotations<?> nodeWithAnnotations
          && !nodeWithAnnotations.getAnnotations().isEmpty()
          && hasNotNullOrNonnullAnnotation(nodeWithAnnotations.getAnnotations())) {
        nodesWithNotNullAnnotations.add((Node) nodeWithAnnotations);
      }
    }
    return nodesWithNotNullAnnotations;
  }

  /** Checks if the provided list of annotations contains any @NotNull or @Nonnull annotations. */
  private boolean hasNotNullOrNonnullAnnotation(final NodeList<AnnotationExpr> annotations) {
    for (final AnnotationExpr annotation : annotations) {
      if (isNotNullOrNonnullAnnotation(annotation)) {
        return true;
      }
    }
    return false;
  }

  private boolean isNotNullOrNonnullAnnotation(final AnnotationExpr annotation) {
    final Name annotationName = annotation.getName();
    final String name = annotationName.getIdentifier();
    return "NotNull".equals(name) || "Nonnull".equals(name);
  }

  /**
   * Checks if the provided {@code SimpleName} variable corresponds to a VariableDeclarator that was
   * previously initialized to a non-null value.
   */
  private boolean isSimpleNameANotNullInitializedVariableDeclarator(
      final List<VariableDeclarator> variableDeclarators, final SimpleName targetName) {

    if (targetName != null) {
      for (final VariableDeclarator declarator : variableDeclarators) {
        final SimpleName declaratorSimpleName = declarator.getName();
        if (declaratorSimpleName.equals(targetName)
            && isPreviousNodeBefore(targetName, declaratorSimpleName)) {
          final Optional<Expression> initializer = declarator.getInitializer();
          return initializer.isPresent() && !(initializer.get() instanceof NullLiteralExpr);
        }
      }
    }

    return false;
  }

  /**
   * Retrieves the SimpleName from the given MethodCallExpr. We know that this codemod only filters
   * MethodCallExpr nodes that has one SimpleName node.
   */
  private SimpleName getSimpleNameFromMethodCallExpr(final MethodCallExpr methodCallExpr) {

    final List<SimpleName> simpleNames = new ArrayList<>();

    // Get the arguments of the MethodCallExpr
    final List<Node> childNodes = methodCallExpr.getChildNodes();

    // Iterate through the arguments and collect NameExpr nodes
    for (final Node node : childNodes) {
      if (node instanceof NameExpr nameExpr) {
        simpleNames.add(nameExpr.getName());
      }
    }

    return !simpleNames.isEmpty() ? simpleNames.get(0) : null;
  }

  private static final Set<String> flippableComparisonMethods =
      Set.of("equals", "equalsIgnoreCase");
}
