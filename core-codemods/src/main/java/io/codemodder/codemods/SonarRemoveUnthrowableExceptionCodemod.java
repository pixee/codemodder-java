package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssue;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.sonar.model.Issue;
import javax.inject.Inject;

/** A codemod for automatically removing unthrowable exceptions. */
@Codemod(
    id = "sonar:java/remove-unthrowable-exceptions-s1130",
    reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW,
    importance = Importance.LOW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class SonarRemoveUnthrowableExceptionCodemod
    extends SonarPluginJavaParserChanger<ClassOrInterfaceType, Issue> {

  @Inject
  public SonarRemoveUnthrowableExceptionCodemod(
      @ProvidedSonarScan(ruleId = "java:S1130") final RuleIssue issues) {
    super(issues, ClassOrInterfaceType.class);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final ClassOrInterfaceType type,
      final Issue issue) {
    type.remove();
    return ChangesResult.changesApplied;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S1130",
        "Exceptions in \"throws\" clauses should not be superfluous",
        "https://rules.sonarsource.com/java/RSPEC-1130/");
  }
}
