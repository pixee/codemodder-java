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

    final List<SimpleName> simpleNames = getSimpleNames(methodCallExpr);

    final SimpleName simpleName = !simpleNames.isEmpty() ? simpleNames.get(0) : null;

    // No need to switch order because variable was declared and initialized to a not null value
    if (isSimpleNameANotNullInitializedVariableDeclarator(variableDeclarators, simpleName)) {
      return false;
    }

    final List<FieldDeclaration> fieldDeclarations = cu.findAll(FieldDeclaration.class);
    final List<Parameter> parameters = cu.findAll(Parameter.class);
    // Create a new list to collect all annotation nodes
    final List<Node> annotationNodesCandidates = new ArrayList<>();
    annotationNodesCandidates.addAll(variableDeclarators);
    annotationNodesCandidates.addAll(fieldDeclarations);
    annotationNodesCandidates.addAll(parameters);

    final List<Node> annotationNodes = filterNodesWithNotNullAnnotations(annotationNodesCandidates);

    if (hasSimpleNameNotNullAnnotation(annotationNodes, simpleName)) {
      return false;
    }

    final List<NullLiteralExpr> nullLiteralExprs = cu.findAll(NullLiteralExpr.class);

    if (hasSimpleNamePreviousNullAssertion(nullLiteralExprs, simpleName)) {
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

  private boolean isPreviousNodeBefore(final Node nameNode, final Node previousNode) {
    final Optional<Range> nameNodeRange = nameNode.getRange();
    final Optional<Range> previosNodeRange = previousNode.getRange();
    if (nameNodeRange.isEmpty() || previosNodeRange.isEmpty()) {
      return false;
    }
    return previosNodeRange.get().begin.isBefore(nameNodeRange.get().begin);
  }

  private boolean hasSimpleNamePreviousNullAssertion(
      final List<NullLiteralExpr> nullLiterals, final SimpleName name) {

    if (nullLiterals != null && !nullLiterals.isEmpty()) {
      for (final NullLiteralExpr nullLiteralExpr : nullLiterals) {
        if (nullLiteralExpr.getParentNode().isPresent()
            && nullLiteralExpr.getParentNode().get() instanceof BinaryExpr parentBinaryExpr) {
          if (parentBinaryExpr.getOperator() == BinaryExpr.Operator.NOT_EQUALS) {
            final Node left = parentBinaryExpr.getLeft();
            final Node right = parentBinaryExpr.getRight();
            if (isBinaryNodeChildEqualToSimpleName(left, name)
                || isBinaryNodeChildEqualToSimpleName(right, name)) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  private boolean isBinaryNodeChildEqualToSimpleName(final Node child, final SimpleName name) {
    return child instanceof NameExpr nameExpr
        && nameExpr.getName().equals(name)
        && isPreviousNodeBefore(name, nameExpr.getName());
  }

  private boolean hasSimpleNameNotNullAnnotation(
      final List<Node> annotations, final SimpleName simpleName) {

    if (annotations != null && !annotations.isEmpty()) {
      for (final Node annotation : annotations) {

        if (annotation instanceof Parameter || annotation instanceof VariableDeclarator) {
          final SimpleName annotationSimpleName = ((NodeWithSimpleName<?>) annotation).getName();
          if (annotationSimpleName.equals(simpleName)
              && isPreviousNodeBefore(simpleName, annotationSimpleName)) {
            return true;
          }
        } else if (annotation instanceof FieldDeclaration fieldDeclaration) {
          final List<VariableDeclarator> variableDeclarators = fieldDeclaration.getVariables();
          for (final VariableDeclarator variableDeclarator : variableDeclarators) {
            final SimpleName variableSimpleName = variableDeclarator.getName();
            if (variableSimpleName.equals(simpleName)
                && isPreviousNodeBefore(simpleName, variableSimpleName)) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

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

  private final boolean isSimpleNameANotNullInitializedVariableDeclarator(
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

  private List<SimpleName> getSimpleNames(final MethodCallExpr methodCallExpr) {
    final List<SimpleName> nameExprNodes = new ArrayList<>();

    // Get the arguments of the MethodCallExpr
    final List<Node> childNodes = methodCallExpr.getChildNodes();

    // Iterate through the arguments and collect NameExpr nodes
    for (final Node node : childNodes) {
      if (node instanceof NameExpr nameExpr) {
        nameExprNodes.add(nameExpr.getName());
      }
    }

    return nameExprNodes;
  }

  private static final Set<String> flippableComparisonMethods =
      Set.of("equals", "equalsIgnoreCase");
}
