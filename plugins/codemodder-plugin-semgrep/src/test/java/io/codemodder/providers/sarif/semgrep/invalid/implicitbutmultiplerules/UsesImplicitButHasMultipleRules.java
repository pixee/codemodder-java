package io.codemodder.providers.sarif.semgrep.invalid.implicitbutmultiplerules;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.*;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import javax.inject.Inject;

/**
 * An invalid codemod that uses an implicit rule but has multiple rules, so it's not deducible which
 * rule is the one to use.
 */
@Codemod(
    id = "pixee-test:java/uses-implicit-rule",
    importance = Importance.LOW,
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
public class UsesImplicitButHasMultipleRules
    extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

  private static final String YAML_MISSING_STUFF =
      """
            rules:
              - id: explicit-yaml-path
                pattern: new Stuff()
              - id: explicit-yaml-path-also
                pattern: new Bar()
            """;

  @Inject
  UsesImplicitButHasMultipleRules(@SemgrepScan(yaml = YAML_MISSING_STUFF) RuleSarif ruleSarif) {
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
