package io.codemodder.codemods;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import io.github.pixee.security.Reflection;
import java.util.List;
import javax.inject.Inject;

/** Sonar remediation codemod for S2658: Classes should not be loaded dynamically. */
@Codemod(
    id = "sonar:java/unsafe-reflection-s2658",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class UnsafeReflectionRemediationCodemod
    extends SonarPluginJavaParserChanger<MethodCallExpr, Issue> {

  @Inject
  public UnsafeReflectionRemediationCodemod(
      @ProvidedSonarScan(ruleId = "java:S2658") final RuleIssue issues) {
    super(
        issues,
        MethodCallExpr.class,
        RegionNodeMatcher.REGION_INSIDE_RANGE,
        NodeCollector.ALL_FROM_TYPE);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Issue issue) {

    // check method expression scope and name, because the region match is fuzzy
    if (!(isClassForNameCall(cu, methodCallExpr))) {
      return ChangesResult.noChanges;
    }

    replaceMethodCallExpression(cu, methodCallExpr);
    return ChangesResult.changesAppliedWith(List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S2658",
        "Classes should not be loaded dynamically",
        "https://rules.sonarsource.com/java/RSPEC-2658/");
  }

  /**
   * Updates the scope and name of the method call expression to {@code Reflection.loadAndVerify},
   * and adds the import if missing.
   *
   * @param cu CompilationUnit to update with imports
   * @param methodCallExpr the method call expression to update
   */
  private static void replaceMethodCallExpression(
      final CompilationUnit cu, final MethodCallExpr methodCallExpr) {
    final var name = new NameExpr(Reflection.class.getSimpleName());
    methodCallExpr.setScope(name);
    methodCallExpr.setName("loadAndVerify");
    addImportIfMissing(cu, Reflection.class);
  }

  /**
   * Check if the method call expression is a call to {@code Class.forName(String)}.
   *
   * <p>This is important, because this codemod uses fuzzy region matching, to account for how Sonar
   * reports the region for this finding. Sonar reports the region to be only the "forName" part of
   * the method expression.
   *
   * @param cu CompilationUnit for checking static imports
   * @param methodCallExpr the method call expression to check
   * @return true if the method call expression is a call to {@code Class.forName(String)}
   */
  private static boolean isClassForNameCall(
      final CompilationUnit cu, final MethodCallExpr methodCallExpr) {
    final boolean scopeMatches =
        methodCallExpr
            .getScope()
            .map(
                expression -> {
                  if (expression.isNameExpr()) {
                    final var nameExpr = expression.asNameExpr();
                    return nameExpr.getNameAsString().equals("Class")
                        || nameExpr.getNameAsString().equals("java.lang.Class");
                  }
                  return false;
                })
            .orElse(
                // check for static import (TODO test)
                cu.getImports().stream()
                    .anyMatch(
                        importDeclaration ->
                            importDeclaration.isStatic()
                                && importDeclaration
                                    .getNameAsString()
                                    .equals("java.lang.Class.forName")));
    final var methodNameMatches = methodCallExpr.getNameAsString().equals("forName");
    return scopeMatches && methodNameMatches;
  }
}
