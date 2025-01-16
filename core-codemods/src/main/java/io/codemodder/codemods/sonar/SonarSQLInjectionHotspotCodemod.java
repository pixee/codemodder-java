package io.codemodder.codemods.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleHotspot;
import io.codemodder.providers.sonar.SonarRemediatingJavaParserChanger;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.SearcherStrategyRemediator;
import io.codemodder.remediation.sqlinjection.SQLInjectionFixComposer;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.SonarFinding;
import io.codemodder.sonar.model.TextRange;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

@Codemod(
    id = "sonar:java/sonar-sql-injection-s2077",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarSQLInjectionHotspotCodemod extends SonarRemediatingJavaParserChanger {

  private final Remediator<Hotspot> remediationStrategy;
  private final RuleHotspot hotspots;

  @Inject
  public SonarSQLInjectionHotspotCodemod(
      @ProvidedSonarScan(ruleId = "java:S2077") final RuleHotspot hotspots) {
    super(GenericRemediationMetadata.SQL_INJECTION.reporter(), hotspots);
    this.hotspots = Objects.requireNonNull(hotspots);
    this.remediationStrategy =
        new SearcherStrategyRemediator.Builder<Hotspot>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<Hotspot>()
                    .withMatcher(
                        n ->
                            Optional.empty()
                                // is the argument of the call
                                .or(
                                    () ->
                                        Optional.of(n)
                                            .map(
                                                m ->
                                                    m instanceof Expression ? (Expression) m : null)
                                            .flatMap(ASTs::isArgumentOfMethodCall)
                                            .filter(SQLInjectionFixComposer::match))
                                .isPresent())
                    .build(),
                new SQLInjectionFixComposer())
            .build();
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
        i -> i.getTextRange() != null ? i.getTextRange().getStartLine() : i.getLine(),
        i -> Optional.ofNullable(i.getTextRange()).map(TextRange::getEndLine),
        i -> Optional.ofNullable(i.getTextRange()).map(tr -> tr.getStartOffset() + 1));
  }
}
