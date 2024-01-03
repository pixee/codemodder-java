package io.codemodder.codemods;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.comments.Comment;
import io.codemodder.*;
import io.codemodder.providers.sonar.ProvidedSonarScan;
import io.codemodder.providers.sonar.RuleIssues;
import io.codemodder.providers.sonar.SonarPluginJavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import java.util.List;
import javax.inject.Inject;

/** A codemod for removing commented-out lines of code. */
@Codemod(
    id = "sonar:java/remove-commented-code-s125",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW,
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

  @Override
  public List<? extends Node> getAllCompilationUnitNodes(final CompilationUnit cu) {
    return cu.getAllComments();
  }
}
