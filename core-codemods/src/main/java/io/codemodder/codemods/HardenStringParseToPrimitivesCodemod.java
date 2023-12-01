package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
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
    executionPriority = CodemodExecutionPriority.HIGH)
public final class HardenStringParseToPrimitivesCodemod extends CompositeJavaParserChanger {

  // TODO create another static sonar java parser changer to handle constructor cases
  // (ObjectCreationExpr)
  @Inject
  public HardenStringParseToPrimitivesCodemod(
      final HardenParseForValueOfChanger parseMethodCallExprChanger) {
    super(parseMethodCallExprChanger);
  }

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

        final String replacementMethod = determineParsingMethodForType(targetType);

        if (replacementMethod != null) {
          methodCallExpr.setName(replacementMethod);

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

    private String determineParsingMethodForType(final String type) {
      return switch (type) {
        case "java.lang.Integer" -> "parseInt";
        case "java.lang.Float" -> "parseFloat";
          // Add more cases if needed
        default -> null;
      };
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
