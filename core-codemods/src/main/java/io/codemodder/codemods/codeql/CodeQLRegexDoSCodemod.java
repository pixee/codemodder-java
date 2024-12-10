package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.regexdos.RegexDoSRemediator;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod that mitigates regex dos vulnerabilities * */
@Codemod(
    id = "codeql:java/regex-dos",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLRegexDoSCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLRegexDoSCodemod(
      @ProvidedCodeQLScan(ruleId = "java/polynomial-redos") final RuleSarif sarif) {
    super(GenericRemediationMetadata.REGEX_DOS.reporter(), sarif);
    this.remediator = new RegexDoSRemediator<>();
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "polynomial-redos",
        "Polynomial regular expression used on uncontrolled data",
        "https://codeql.github.com/codeql-query-help/java/java-polynomial-redos/");
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
        r ->
            Optional.ofNullable(
                r.getLocations().get(0).getPhysicalLocation().getRegion().getEndLine()),
        r ->
            Optional.ofNullable(
                r.getLocations().get(0).getPhysicalLocation().getRegion().getStartColumn()));
  }
}
