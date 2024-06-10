package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.Importance;
import io.codemodder.ReviewGuidance;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleFinding;
import io.codemodder.providers.sonar.SonarFindingType;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import triage.SonarFinding;

import javax.inject.Inject;

@Codemod(
        id = "sonar:java/sonar-sql-injection-s2077",
        reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
        importance = Importance.HIGH,
        executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarSQLInjectionCodemod extends SonarPluginJavaParserChanger<Node> {

    @Inject
    public SonarSQLInjectionCodemod(
            @ProvidedSonarScan(type = SonarFindingType.HOTSPOT, ruleId = "java:S2077")
            final RuleFinding hotspots) {
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
    public ChangesResult onFindingFound(CodemodInvocationContext context, CompilationUnit cu, Node node, SonarFinding sonarFinding) {
        node.remove();
        return ChangesResult.changesApplied;
    }
}
