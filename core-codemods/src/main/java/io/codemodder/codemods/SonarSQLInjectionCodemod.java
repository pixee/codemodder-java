package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.Importance;
import io.codemodder.ReviewGuidance;
import io.codemodder.codemods.util.JavaParserSQLInjectionRemediatorStrategy;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleHotspot;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.SonarFinding;
import java.util.List;
import javax.inject.Inject;

@Codemod(
    id = "sonar:java/sonar-sql-injection-s2077",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarSQLInjectionCodemod extends SonarPluginJavaParserChanger<Node, Hotspot> {

  @Inject
  public SonarSQLInjectionCodemod(
      @ProvidedSonarScan(ruleId = "java:S2077") final RuleHotspot hotspots) {
    super(hotspots, Node.class);
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S2077",
        "Formatting SQL queries is security-sensitive",
        "https://rules.sonarsource.com/java/RSPEC-2077/");
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<Hotspot> hotspots = ruleFinding.getResultsByPath(context.path());

    return JavaParserSQLInjectionRemediatorStrategy.DEFAULT.visit(
        context, cu, hotspots, detectorRule(), SonarFinding::getKey, SonarFinding::getLine);
  }

  @Override
  protected ChangesResult onFindingFound(
      CodemodInvocationContext context, CompilationUnit cu, Node node, Hotspot sonarFinding) {
    return null;
  }
}
