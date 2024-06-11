package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.SimpleName;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleFinding;
import io.codemodder.providers.sonar.SonarFindingType;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.SonarFinding;
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
      @ProvidedSonarScan(type = SonarFindingType.ISSUE, ruleId = "java:S5361")
          final RuleFinding issues) {
    super(issues, SimpleName.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final SimpleName name,
      final SonarFinding sonarFinding) {
    name.setIdentifier("replace");
    return ChangesResult.changesApplied;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S5361",
        "`String#replace` should be preferred to `String#replaceAll`",
        "https://rules.sonarsource.com/java/RSPEC-5361");
  }
}
