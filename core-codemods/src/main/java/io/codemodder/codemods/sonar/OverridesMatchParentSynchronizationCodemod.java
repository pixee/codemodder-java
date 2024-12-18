package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import java.util.Optional;
import javax.inject.Inject;

/**
 * A codemod for automatically fixing overridden methods that do not match their parent methods in
 * synchronization.
 */
@Codemod(
    id = "sonar:java/overrides-match-synchronization-s3551",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class OverridesMatchParentSynchronizationCodemod
    extends SonarPluginJavaParserChanger<SimpleName, Issue> {

  @Inject
  public OverridesMatchParentSynchronizationCodemod(
      @ProvidedSonarScan(ruleId = "java:S3551") final RuleIssue issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public ChangesResult onFindingFound(
      CodemodInvocationContext context, CompilationUnit cu, SimpleName methodName, Issue issue) {
    Optional<Node> parentNodeRef = methodName.getParentNode();
    if (parentNodeRef.isPresent()) {
      Node parentNode = parentNodeRef.get();
      if (parentNode instanceof MethodDeclaration method) {
        method.setSynchronized(true);
        return ChangesResult.changesApplied;
      }
    }
    return ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S3551",
        "Overrides should match their parent class methods in synchronization",
        "https://rules.sonarsource.com/java/RSPEC-3551");
  }
}
