package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codemods.util.JavaParserSQLInjectionRemediatorStrategy;
import io.codemodder.codemods.util.ParameterizedJavaParserSQLInjectionRemediatorStrategy;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleHotspot;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.SonarFinding;
import java.util.List;
import java.util.Objects;
import javax.inject.Inject;

@Codemod(
    id = "sonar:java/sonar-table-injection-filter-s2077",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarSQLTableInjectionFilterCodemod extends SonarRemediatingJavaParserChanger {

  private final JavaParserSQLInjectionRemediatorStrategy remediationStrategy;
  private final RuleHotspot hotspots;

  @Inject
  public SonarSQLTableInjectionFilterCodemod(
      @ProvidedSonarScan(ruleId = "java:S2077") final RuleHotspot hotspots) {
    super(GenericRemediationMetadata.SQL_INJECTION.reporter(), hotspots);
    this.hotspots = Objects.requireNonNull(hotspots);
    this.remediationStrategy =
        new ParameterizedJavaParserSQLInjectionRemediatorStrategy(
            SQLTableInjectionFilterTransform::matchCall, SQLTableInjectionFilterTransform::fix);
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
    List<Hotspot> hotspotsForFile = hotspots.getResultsByPath(context.path());
    return remediationStrategy.remediateAll(
        cu,
        context.path().toString(),
        detectorRule(),
        hotspotsForFile,
        SonarFinding::getKey,
        SonarFinding::getLine);
  }
}
