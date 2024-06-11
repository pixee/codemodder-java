package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.EnumDeclaration;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleFinding;
import io.codemodder.providers.sonar.SonarFindingType;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.SonarFinding;
import javax.inject.Inject;

/** A codemod for automatically removing redundant static flags on nested enums. */
@Codemod(
    id = "sonar:java/remove-redundant-static-s2786",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class FixRedundantStaticOnEnumCodemod
    extends SonarPluginJavaParserChanger<EnumDeclaration> {

  @Inject
  public FixRedundantStaticOnEnumCodemod(
      @ProvidedSonarScan(type = SonarFindingType.ISSUE, ruleId = "java:S2786")
          final RuleFinding issues) {
    super(issues, EnumDeclaration.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final EnumDeclaration enumDecl,
      final SonarFinding sonarFinding) {
    if (enumDecl.isStatic()) {
      enumDecl.setStatic(false);
      return ChangesResult.changesApplied;
    }
    return ChangesResult.noChanges;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S2786",
        "Nested `enum`s should not be declared static",
        "https://rules.sonarsource.com/java/RSPEC-2786/");
  }
}
