package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
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
    extends SonarPluginJavaParserChanger<MethodCallExpr, Issue> {

  @Inject
  public ReplaceStreamCollectorsToListCodemod(
      @ProvidedSonarScan(ruleId = "java:S6204") final RuleIssue issues) {
    super(issues, MethodCallExpr.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final MethodCallExpr methodCallExpr,
      final Issue issue) {

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
