package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
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

/** A codemod for replacing 'Stream.collect(Collectors.toList())' with 'Stream.toList()' */
@Codemod(
    id = "sonar:java/replace-stream-collectors-to-list-s6204",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class ReplaceStreamCollectorsToListCodemod
    extends SonarIssuesPluginJavaParserChanger<MethodCallExpr> {

  @Inject
  public ReplaceStreamCollectorsToListCodemod(
      @ProvidedSonarScan(type = SonarFindingType.ISSUE, ruleId = "java:S6204")
          final RuleFinding issues) {
    super(issues, MethodCallExpr.class);
  }

  @Override
  public ChangesResult onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Issue sonarFinding) {

    final Optional<Node> collectMethodExprOptional = methodCallExpr.getParentNode();

    if (collectMethodExprOptional.isEmpty()) {
      return ChangesResult.noChanges;
    }

    final MethodCallExpr collectMethodExpr = (MethodCallExpr) collectMethodExprOptional.get();
    collectMethodExpr.setName("toList");

    collectMethodExpr.setArguments(new NodeList<>());

    return ChangesResult.changesApplied;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S6204",
        "`Stream.toList()` should be used instead of `collectors`",
        "https://rules.sonarsource.com/java/RSPEC-6204");
  }
}
