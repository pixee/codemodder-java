package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import io.codemodder.Codemod;
import io.codemodder.CodemodExecutionPriority;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.Importance;
import io.codemodder.NodeCollector;
import io.codemodder.RegionNodeMatcher;
import io.codemodder.ReviewGuidance;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

@Codemod(
    id = "sonar:java/remove-unused-import-s1128",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveUnusedImportCodemod
    extends SonarPluginJavaParserChanger<ImportDeclaration> {

  @Inject
  public RemoveUnusedImportCodemod(
      @ProvidedSonarScan(ruleId = "java:S1128") final RuleIssues issues) {
    super(
        issues,
        ImportDeclaration.class,
        /*
         * This codemod uses a less strict RegionNodeMatcher because the import declaration technically starts on column
         * 1 according to java parser but sonar reports the unused import starting from the import keyword, on column 8,
         * which is where the unused name is actually located. Neither are technically incorrect, but we can be less
         * strict with matching here given how relatively simple the import declaration is.
         */
        RegionNodeMatcher.MATCHES_LINE,
        NodeCollector.ALL_FROM_TYPE);
  }

  @Override
  protected DetectorRule getDetectorRule() {
    return new DetectorRule(
        "java:S1128",
        "Unnecessary imports should be removed",
        "https://rules.sonarsource.com/java/RSPEC-1128");
  }

  @Override
  public ChangesResult onIssueFound(
      CodemodInvocationContext context, CompilationUnit cu, ImportDeclaration node, Issue issue) {
    if (issue.getMessage().contains(node.getNameAsString())) {
      return cu.remove(node) ? ChangesResult.changesApplied : ChangesResult.noChanges;
    } else {
      return ChangesResult.noChanges;
    }
  }
}