package io.codemodder.codemods;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AnnotationExpr;
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
    final List<FieldDeclaration> fieldDeclarations = cu.findAll(FieldDeclaration.class);
    final List<Parameter> parameters = cu.findAll(Parameter.class);

    final List<SimpleName> simpleNames = getSimpleNames(methodCallExpr);

    final SimpleName simpleName = !simpleNames.isEmpty() ? simpleNames.get(0) : null;

    // No need to switch order because variable was declared and initialized to a not null value
    if (isSimpleNameANotNullInitializedVariableDeclarator(variableDeclarators, simpleName)) {
      return false;
    }

    // Create a new list to collect all annotation nodes
    List<Node> annotationNodesCandidates = new ArrayList<>();
    annotationNodesCandidates.addAll(variableDeclarators);
    annotationNodesCandidates.addAll(fieldDeclarations);
    annotationNodesCandidates.addAll(parameters);

    List<Node> annotationNodes = filterNodesWithNotNullAnnotations(annotationNodesCandidates);

    if (hasSimpleNameNotNullAnnotation(annotationNodes, simpleName)) {
      return false;
    }

    Expression leftSide = methodCallExpr.getScope().get();
    Expression rightSide = methodCallExpr.getArgument(0);
    try {
      ResolvedType leftType = leftSide.calculateResolvedType();
      if ("Ljava/lang/String;".equals(leftType.toDescriptor())) {
        methodCallExpr.setScope(rightSide);
        methodCallExpr.setArgument(0, leftSide);
        return true;
      }
    } catch (UnsolvedSymbolException e) {
      // expected in cases where we can't resolve the type
    }

    return false;
  }

  public static boolean hasSimpleNameNotNullAnnotation(
      List<Node> annotations, SimpleName simpleName) {

    if (annotations != null && !annotations.isEmpty()) {
      for (Node annotation : annotations) {

        if (annotation instanceof Parameter || annotation instanceof VariableDeclarator) {
          if (((NodeWithSimpleName<?>) annotation).getName().equals(simpleName)) {
            return true;
          }
        } else if (annotation instanceof FieldDeclaration fieldDeclaration) {
          final List<VariableDeclarator> variableDeclarators = fieldDeclaration.getVariables();
          for (VariableDeclarator variableDeclarator : variableDeclarators) {
            if (variableDeclarator.getName().equals(simpleName)) {
              return true;
            }
          }
        }
      }
    }

    return false;
  }

  public static List<Node> filterNodesWithNotNullAnnotations(List<Node> annotationNodes) {
    List<Node> nodesWithNotNullAnnotations = new ArrayList<>();
    for (Node node : annotationNodes) {
      if (node instanceof NodeWithAnnotations<?> nodeWithAnnotations
          && !nodeWithAnnotations.getAnnotations().isEmpty()
          && hasNotNullOrNonnullAnnotation(nodeWithAnnotations.getAnnotations())) {
        nodesWithNotNullAnnotations.add((Node) nodeWithAnnotations);
      }
    }
    return nodesWithNotNullAnnotations;
  }

  public static boolean hasNotNullOrNonnullAnnotation(NodeList<AnnotationExpr> annotations) {
    for (AnnotationExpr annotation : annotations) {
      if (isNotNullOrNonnullAnnotation(annotation)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isNotNullOrNonnullAnnotation(AnnotationExpr annotation) {

    Name annotationName = annotation.getName();
    String name = annotationName.getIdentifier();
    return "NotNull".equals(name) || "Nonnull".equals(name);
  }

  public static boolean isSimpleNameANotNullInitializedVariableDeclarator(
      List<VariableDeclarator> variableDeclarators, SimpleName targetName) {

    if (targetName != null) {
      for (VariableDeclarator declarator : variableDeclarators) {
        if (declarator.getName().equals(targetName)) {
          final Optional<Expression> initializer = declarator.getInitializer();
          return initializer.isPresent() && !(initializer.get() instanceof NullLiteralExpr);
        }
      }
    }

    return false;
  }

  public static List<SimpleName> getSimpleNames(MethodCallExpr methodCallExpr) {
    List<SimpleName> nameExprNodes = new ArrayList<>();

    // Get the arguments of the MethodCallExpr
    List<Node> childNodes = methodCallExpr.getChildNodes();

    // Iterate through the arguments and collect NameExpr nodes
    for (Node node : childNodes) {
      if (node instanceof NameExpr) {
        final NameExpr nameExpr = (NameExpr) node;
        nameExprNodes.add(nameExpr.getName());
      }
    }

    return nameExprNodes;
  }

  private static final Set<String> flippableComparisonMethods =
      Set.of("equals", "equalsIgnoreCase");
}
