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

  @Inject
  public RemoveCommentedCodeCodemod(
      @ProvidedSonarScan(ruleId = "java:S125") final RuleIssues issues) {
    super(issues, Comment.class, RegionNodeMatcher.TEST);
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
