package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import javax.inject.Inject;

/** A codemod for automatically fixing missing @Override annotations. */
@Codemod(
    id = "sonar:java/add-missing-override-s1161",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class AddMissingOverrideCodemod
    extends SonarPluginJavaParserChanger<SimpleName, Issue> {

  @Inject
  public AddMissingOverrideCodemod(
      @ProvidedSonarScan(ruleId = "java:S1161") final RuleIssue issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName methodName,
      final Issue issue) {

    var maybeMethodName =
        methodName
            .getParentNode()
            .map(p -> p instanceof MethodDeclaration ? (MethodDeclaration) p : null)
            .filter(mr -> !mr.getAnnotationByName("Override").isPresent());
    maybeMethodName.ifPresent(mr -> mr.addAnnotation(Override.class));
    return maybeMethodName.map(mr -> ChangesResult.changesApplied).orElse(ChangesResult.noChanges);
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1161",
        "`@Override` should be used on overriding and implementing methods",
        "https://rules.sonarsource.com/java/RSPEC-1161/");
  }
}
