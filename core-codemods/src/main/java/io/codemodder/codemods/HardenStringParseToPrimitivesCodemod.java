package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import java.util.Optional;
import javax.inject.Inject;

/**
 * A codemod that enforces the appropriate parsing technique for converting Strings to primitive
 * types in the codebase.
 */
@Codemod(
    id = "sonar:java/harden-string-parse-to-primitives-s2130",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class HardenStringParseToPrimitivesCodemod extends CompositeJavaParserChanger {

  @Inject
  public HardenStringParseToPrimitivesCodemod(
      final HardenParseForConstructorChanger hardenParseForConstructorChanger,
      final HardenParseForValueOfChanger hardenParseForValueOfChanger) {
    super(hardenParseForConstructorChanger, hardenParseForValueOfChanger);
  }

  private static Optional<String> determineParsingMethodForType(final String type) {
    if ("java.lang.Integer".equals(type) || "Integer".equals(type)) {
      return Optional.of("parseInt");
    }

    if ("java.lang.Float".equals(type) || "Float".equals(type)) {
      return Optional.of("parseFloat");
    }

    return Optional.empty();
  }

  /**
   * Handles cases where Strings are converted to numbers using constructors like new Integer("24")
   * or new Float("12").
   */
  private static final class HardenParseForConstructorChanger
      extends SonarPluginJavaParserChanger<ObjectCreationExpr> {

    @Inject
    public HardenParseForConstructorChanger(
        @ProvidedSonarScan(ruleId = "java:S2130") final RuleIssues issues) {
      super(
          issues,
          ObjectCreationExpr.class,
          RegionNodeMatcher.MATCHES_START,
          CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onIssueFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final ObjectCreationExpr objectCreationExpr,
        final Issue issue) {

      final String type = objectCreationExpr.getType().asString();
      final Expression argumentExpression = objectCreationExpr.getArguments().get(0);

      final Optional<Expression> argument = extractArgumentExpression(argumentExpression);

      final Optional<String> replacementMethod = determineParsingMethodForType(type);

      if (replacementMethod.isPresent() && argument.isPresent()) {
        MethodCallExpr replacementExpr =
            new MethodCallExpr(new NameExpr(type), replacementMethod.get());

        replacementExpr.addArgument(argument.get());

        objectCreationExpr.replace(replacementExpr);
        return true;
      }

      return false;
    }

    private Optional<Expression> extractArgumentExpression(Expression argumentExpression) {
      if (argumentExpression instanceof StringLiteralExpr
          || argumentExpression instanceof NameExpr) {
        return Optional.of(argumentExpression);
      }
      // Handle other cases or return null if unable to extract the argument expression
      return Optional.empty();
    }
  }

  /**
   * Handles cases where Strings are converted to numbers using the static method .valueOf() from
   * Integer or Float classes.
   */
  private static final class HardenParseForValueOfChanger
      extends SonarPluginJavaParserChanger<MethodCallExpr> {

    @Inject
    public HardenParseForValueOfChanger(
        @ProvidedSonarScan(ruleId = "java:S2130") final RuleIssues issues) {
      super(
          issues,
          MethodCallExpr.class,
          RegionNodeMatcher.MATCHES_START,
          CodemodReporterStrategy.empty());
    }

    @Override
    public boolean onIssueFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final MethodCallExpr methodCallExpr,
        final Issue issue) {

      final String methodName = methodCallExpr.getNameAsString();

      if ("valueOf".equals(methodName)) {
        final String targetType = retrieveTargetTypeFromMethodCallExpr(methodCallExpr);

        final Optional<String> replacementMethod = determineParsingMethodForType(targetType);

        if (replacementMethod.isPresent()) {
          methodCallExpr.setName(replacementMethod.get());

          return handleMethodCallChainsAfterValueOfIfNeeded(methodCallExpr);
        }
      }

      return false;
    }

    private String retrieveTargetTypeFromMethodCallExpr(final MethodCallExpr methodCallExpr) {
      final Optional<NodeList<Type>> optionalTypeArguments = methodCallExpr.getTypeArguments();
      return optionalTypeArguments.isEmpty()
          ? methodCallExpr
              .getScope()
              .map(expr -> expr.calculateResolvedType().describe())
              .orElse("")
          : optionalTypeArguments.get().get(0).toString();
    }

    private boolean handleMethodCallChainsAfterValueOfIfNeeded(
        final MethodCallExpr methodCallExpr) {

      final Optional<Node> optionalParentNode = methodCallExpr.getParentNode();
      if (optionalParentNode.isPresent()
          && optionalParentNode.get() instanceof MethodCallExpr parentMethodCall) {

        final String parentMethodName = parentMethodCall.getNameAsString();

        final Optional<Expression> optionalScope = parentMethodCall.getScope();
        if (optionalScope.isEmpty()) {
          return false;
        }

        if ("intValue".equals(parentMethodName) || "floatValue".equals(parentMethodName)) {
          parentMethodCall.replace(optionalScope.get());
        }
      }

      return true;
    }
  }
}
