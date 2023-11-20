package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** A codemod for automatically fixing missing @Override annotations. */
@Codemod(
    id = "sonar:java/add-missing-override",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class AddMissingOverrideCodemod
    extends SonarPluginJavaParserChanger<MethodDeclaration> {

  @Inject
  public AddMissingOverrideCodemod(
      @ProvidedSonarScan(ruleId = "java:S1161") final RuleIssues issues) {
    super(issues, MethodDeclaration.class);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodDeclaration method,
      final Issue issue) {
    method.addAnnotation(Override.class);
    return true;
  }
}
