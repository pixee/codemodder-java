package io.codemodder.codemods.codeql;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import io.codemodder.*;
import io.codemodder.ast.ASTs;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.providers.sarif.codeql.ProvidedCodeQLScan;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.GenericRemediationMetadata;
import io.codemodder.remediation.Remediator;
import io.codemodder.remediation.SearcherStrategyRemediator;
import io.codemodder.remediation.sqlinjection.SQLInjectionFixComposer;
import java.util.Optional;
import javax.inject.Inject;

/** A codemod for automatically fixing SQL injection from CodeQL. */
@Codemod(
    id = "codeql:java/sql-injection",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class CodeQLSQLInjectionCodemod extends CodeQLRemediationCodemod {

  private final Remediator<Result> remediator;

  @Inject
  public CodeQLSQLInjectionCodemod(
      @ProvidedCodeQLScan(ruleId = "java/sql-injection") final RuleSarif sarif) {
    super(GenericRemediationMetadata.SQL_INJECTION.reporter(), sarif);
    this.remediator =
        new SearcherStrategyRemediator.Builder<Result>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<Result>()
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
        "sql-injection",
        "Query built from user-controlled sources",
        "https://codeql.github.com/codeql-query-help/java/java-sql-injection/");
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
