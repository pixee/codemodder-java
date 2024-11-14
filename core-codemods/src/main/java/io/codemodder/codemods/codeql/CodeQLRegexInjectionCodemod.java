package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.regexinjection.RegexInjectionRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing Regex Injections from CodeQL. */
@Codemod(
    id = "codeql:java/regex-injection",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLRegexInjectionCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLRegexInjectionCodemod(
      @ProvidedCodeQLScan(ruleId = "java/regex-injection") final RuleSarif sarif) {
    super(GenericRemediationMetadata.REGEX_INJECTION.reporter(), sarif);
    this.remediator = new RegexInjectionRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "regex-injectiom",
        "Regular expression injection",
        "https://codeql.github.com/codeql-query-help/java/java-regex-injection/");
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
        r ->
            Optional.of(
                r.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn()));
  }
}
