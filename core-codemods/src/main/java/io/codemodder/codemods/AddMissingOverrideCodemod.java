package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleFinding;
import io.codemodder.providers.sonar.SonarFindingType;
import io.codemodder.providers.sonar.SonarIssuesPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing missing @Override annotations. */
@Codemod(
    id = "sonar:java/add-missing-override-s1161",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class AddMissingOverrideCodemod
    extends SonarIssuesPluginJavaParserChanger<SimpleName> {

  @Inject
  public AddMissingOverrideCodemod(
      @ProvidedSonarScan(type = SonarFindingType.ISSUE, ruleId = "java:S1161")
          final RuleFinding issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public ChangesResult onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName methodName,
      final Issue issue) {

    Optional<Node> parentNodeRef = methodName.getParentNode();
    if (parentNodeRef.isPresent()) {
      Node parentNode = parentNodeRef.get();
      if (parentNode instanceof MethodDeclaration method) {
        method.addAnnotation(Override.class);
        return ChangesResult.changesApplied;
      }
    }
    return ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1161",
        "`@Override` should be used on overriding and implementing methods",
        "https://rules.sonarsource.com/java/RSPEC-1161/");
  }
}
