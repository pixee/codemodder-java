package io.codemodder.providers.sarif.semgrep.invalid.bothyamlstrategies;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/**
 * This codemod is invalid because it specifies an inline YAML rule and a path to a YAML rule. It's
 * not clear which one should be used.
 */
@Codemod(
    id = "pixee-test:java/both-yaml",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public final class InvalidUsesBothYamlStrategies
    extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

  private static final String FINDS_THAT_SEMGREP_YAML =
      "rules:\n  - id: inline-semgrep\n    pattern: new Stuff()\n    languages:\n      - java\n    message: Semgrep found a match\n    severity: WARNING\n";

  @Inject
  public InvalidUsesBothYamlStrategies(
      @SemgrepScan(
              yaml = FINDS_THAT_SEMGREP_YAML,
              pathToYaml = "/other_dir/explicit-yaml-path.yaml",
              ruleId = "explicit-yaml-path")
          RuleSarif ruleSarif) {
    super(
        ruleSarif,
        ObjectCreationExpr.class,
        SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION,
        RegionNodeMatcher.EXACT_MATCH,
        CodemodReporterStrategy.empty());
  }

  @Override
  public boolean onResultFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ObjectCreationExpr node,
      final Result result) {
    return true;
  }
}
