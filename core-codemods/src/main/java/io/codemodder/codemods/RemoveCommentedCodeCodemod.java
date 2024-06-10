package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import io.codemodder.*;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleFinding;
import io.codemodder.providers.sonar.SonarFindingType;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import javax.inject.Inject;

import triage.SonarFinding;

/**
 * A codemod for removing commented-out lines of code. This codemod has dubious value because Sonar
 * often reports a single line out of many consecutive lines that may have code, so removing that
 * particular line will only result in a half-removed commented, which is potentially more
 * confusing. It also false positives when comments start with common coding tokens.
 */
@Codemod(
    id = "sonar:java/remove-commented-code-s125",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    importance = Importance.HIGH,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveCommentedCodeCodemod extends SonarPluginJavaParserChanger<Comment> {

  /**
   * The reason behind this specific region node matcher is that in the reported column by
   * sonarcloud (range.begin.colum) differs by minus one point that the column reported by the java
   * parser (region.start().column())
   */
  private static final RegionNodeMatcher regionNodeMatcher =
      (region, range) ->
          region.start().line() == range.begin.line
              && region.start().column() >= range.begin.column;

  @Inject
  public RemoveCommentedCodeCodemod(
      @ProvidedSonarScan(type = SonarFindingType.ISSUE, ruleId = "java:S125")
          final RuleFinding issues) {

    super(issues, Comment.class, regionNodeMatcher, NodeCollector.ALL_COMMENTS);
  }

  @Override
  public ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Comment comment,
      final SonarFinding sonarFinding) {

    comment.removeForced();

    return ChangesResult.changesApplied;
  }

  @Override
  public DetectorRule detectorRule() {
    return new DetectorRule(
        "java:S125",
        "Sections of code should not be commented out",
        "https://rules.sonarsource.com/java/RSPEC-125");
  }
}
