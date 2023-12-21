package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import javax.inject.Inject;

/** A codemod for removing commented-out lines of code. */
@Codemod(
    id = "sonar:java/remove-commented-code-s125",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
    executionPriority = CodemodExecutionPriority.HIGH)
public final class RemoveCommentedCodeCodemod extends SonarPluginJavaParserChanger<Comment> {

  private static final RegionNodeMatcher regionNodeMatcher =
      (region, range) ->
          region.start().line() == range.begin.line
              && region.start().column() >= range.begin.column;

  @Inject
  public RemoveCommentedCodeCodemod(
      @ProvidedSonarScan(ruleId = "java:S125") final RuleIssues issues) {

    super(issues, Comment.class, regionNodeMatcher);
  }

  @Override
  public boolean onIssueFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Comment comment,
      final Issue issue) {

    comment.removeForced();

    return true;
  }
}
