package io.codemodder.codemods.semgrep;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.Importance;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.SarifFindingKeyUtil;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.semgrep.ProvidedSemgrepScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.reflectioninjection.ReflectionInjectionRemediator;
import javax.inject.Inject;

/**
 * Fix Semgrep issues reported under the id
 * "java.lang.security.audit.unsafe-reflection.unsafe-reflection".
 */
@Codemod(
    id = "semgrep:java/java.lang.security.audit.unsafe-reflection.unsafe-reflection",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.MEDIUM)
public final class SemgrepReflectionInjectionCodemod extends SemgrepJavaParserChanger {

  private final ReflectionInjectionRemediator remediator;

  @Inject
  public SemgrepReflectionInjectionCodemod(
      @ProvidedSemgrepScan(ruleId = "java.lang.security.audit.unsafe-reflection.unsafe-reflection")
          final RuleSarif sarif) {
    super(GenericRemediationMetadata.REFLECTION_INJECTION.reporter(), sarif);
    this.remediator = ReflectionInjectionRemediator.DEFAULT;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        ruleSarif.getRule(),
        "Reflection Injection",
        "https://semgrep.dev/playground/r/java.lang.security.audit.unsafe-reflection.unsafe-reflection");
  }

  @Override
  public CodemodFileScanningResult visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    return remediator.remediateAll(
        cu,
        context.path().toString(),
        detectorRule(),
        ruleSarif.getResultsByLocationPath(context.path()),
        SarifFindingKeyUtil::buildFindingId,
        r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getStartLine(),
        r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine(),
        r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn());
  }
}
