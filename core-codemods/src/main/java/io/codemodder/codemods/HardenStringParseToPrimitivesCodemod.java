package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;
import java.util.List;

/** A codemod for automatically using the relevant integer parsing method . */
@Codemod(
    id = "sonar:java/parse-int-s2130",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class HardenStringParseToPrimitivesCodemod extends CompositeJavaParserChanger {

  @Inject
  public HardenStringParseToPrimitivesCodemod(
      final HardenStringParseToPrimitivesCodemod.ParseMethodCallExprChanger parseMethodCallExprChanger) {
    super(parseMethodCallExprChanger);
  }

  private static class ParseMethodCallExprChanger
      extends SonarPluginJavaParserChanger<MethodCallExpr> {

    @Inject
    public ParseMethodCallExprChanger(
        @ProvidedSonarScan(ruleId = "java:S2130") final RuleIssues issues) {
      super(issues, MethodCallExpr.class, RegionNodeMatcher.MATCHES_START, CodemodReporterStrategy.empty());
    }

      @Override
      public boolean onIssueFound(
              final CodemodInvocationContext context,
              final CompilationUnit cu,
              final MethodCallExpr methodCallExpr,
              final Issue issue) {

          final String methodName = methodCallExpr.getNameAsString();

          // Checking for method calls that violate the java:S2130 rule
          if ("valueOf".equals(methodName)) {
              String targetType = methodCallExpr.getTypeArguments().isEmpty()
                      ? methodCallExpr.getScope().map(expr -> expr.calculateResolvedType().describe()).orElse("")
                      : methodCallExpr.getTypeArguments().get().get(0).toString();

              // Determine the appropriate parsing method based on the type being parsed
              String replacementMethod;
              switch (targetType) {
                  case "java.lang.Integer":
                      replacementMethod = "parseInt";
                      break;
                  case "java.lang.Float":
                      replacementMethod = "parseFloat";
                      break;
                  // Add more cases for other types as needed
                  default:
                      replacementMethod = null; // Handle unsupported cases
                      break;
              }

              if (replacementMethod != null) {
                  methodCallExpr.setName(replacementMethod);

                  // Handle method call chains (e.g., intValue, floatValue) after valueOf
                  if (methodCallExpr.getParentNode().isPresent() &&
                          methodCallExpr.getParentNode().get() instanceof MethodCallExpr) {
                      MethodCallExpr parentMethodCall = (MethodCallExpr) methodCallExpr.getParentNode().get();
                      String parentMethodName = parentMethodCall.getNameAsString();

                      final Expression a = methodCallExpr.getScope().isPresent() ? methodCallExpr.getScope().get() : null;
                      if (parentMethodName.equals("intValue") || parentMethodName.equals("floatValue")) {
                          parentMethodCall.replace(parentMethodCall.getScope().get());
                      }
                  }

                  return true; // Mark the change as successful
              }
          }

          return false; // No change made
      }
  }
}
