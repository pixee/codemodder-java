package io.codemodder.codemods.semgrep;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.CompositeJavaParserChanger;
import io.codemodder.Importance;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.SarifFindingKeyUtil;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.semgrep.ProvidedSemgrepScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.WithoutScopePositionMatcher;
import io.codemodder.remediation.xxe.XXERemediator;
import java.util.Optional;
import javax.inject.Inject;

/** Fixes some Semgrep XXE issues. */
@Codemod(
    id = "semgrep:java/xxe",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH,
    importance = Importance.HIGH)
public final class SemgrepXXECodemod extends CompositeJavaParserChanger {

  @Inject
  public SemgrepXXECodemod(
      final SemgrepXXEDocumentBuilderFactoryCodemod documentBuilderFactoryCodemod,
      final SemgrepXXESaxParserFactoryCodemod saxParserFactoryCodemod) {
    super(
        GenericRemediationMetadata.XXE.reporter(),
        documentBuilderFactoryCodemod,
        saxParserFactoryCodemod);
  }

  public static class SemgrepXXEDocumentBuilderFactoryCodemod extends SemgrepJavaParserChanger {
    private final Remediator<Result> remediator;

    @Inject
    public SemgrepXXEDocumentBuilderFactoryCodemod(
        @ProvidedSemgrepScan(
                ruleId =
                    "java.lang.security.audit.xxe.documentbuilderfactory-disallow-doctype-decl-missing.documentbuilderfactory-disallow-doctype-decl-missing")
            final RuleSarif sarif) {
      super(GenericRemediationMetadata.WEAK_RANDOM.reporter(), sarif);
      this.remediator = new XXERemediator<>(new WithoutScopePositionMatcher());
    }

    @Override
    public DetectorRule detectorRule() {
      return new DetectorRule(
          ruleSarif.getRule(),
          "XXE",
          "https://semgrep.dev/playground/r/NdT3dLr/java.lang.security.audit.xxe.documentbuilderfactory-disallow-doctype-decl-missing.documentbuilderfactory-disallow-doctype-decl-missing");
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
          r -> Optional.of(r.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine()),
          r -> Optional.empty());
    }
  }

  public static class SemgrepXXESaxParserFactoryCodemod extends SemgrepJavaParserChanger {

    private final Remediator<Result> remediator;

    @Inject
    public SemgrepXXESaxParserFactoryCodemod(
        @ProvidedSemgrepScan(
                ruleId =
                    "java.lang.security.audit.xxe.saxparserfactory-disallow-doctype-decl-missing.saxparserfactory-disallow-doctype-decl-missing")
            final RuleSarif sarif) {
      super(GenericRemediationMetadata.WEAK_RANDOM.reporter(), sarif);
      this.remediator = new XXERemediator<>();
    }

    @Override
    public DetectorRule detectorRule() {
      return new DetectorRule(
          ruleSarif.getRule(),
          "XXE",
          "https://semgrep.dev/playground/r/NdT3dLr/java.lang.security.audit.xxe.saxparserfactory-disallow-doctype-decl-missing.saxparserfactory-disallow-doctype-decl-missing");
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
          r -> Optional.of(r.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine()),
          r -> Optional.empty());
    }
  }
}
