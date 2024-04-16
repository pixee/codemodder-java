package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** A codemod for automatically replacing replaceAll() calls to replace() . */
@Codemod(
    id = "sonar:java/substitute-replaceAll-s5361",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.MEDIUM,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SubstituteReplaceAllCodemod extends SonarPluginJavaParserChanger<SimpleName> {

  @Inject
  public SubstituteReplaceAllCodemod(
      @ProvidedSonarScan(ruleId = "java:S5361") final RuleIssues issues) {
    super(
        issues,
        SimpleName.class,
        new DetectorRule(
            "java:S5361",
            "`String#replace` should be preferred to `String#replaceAll`",
            "https://rules.sonarsource.com/java/RSPEC-5361"));
  }

  @Override
  public ChangesResult onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName name,
      final Issue issue) {
    name.setIdentifier("replace");
    return ChangesResult.changesApplied;
  }
}
