package io.codemodder.providers.sonar;

import com.github.javaparser.Range;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.*;
import io.codemodder.javaparser.JavaParserChanger;
import io.codemodder.providers.sonar.api.Issue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Provides base functionality for making JavaParser-based changes based on Sonar results. */
public abstract class SonarPluginJavaParserChanger<T extends Node> extends JavaParserChanger {

  private final RuleIssues ruleIssues;
  private final Class<? extends Node> nodeType;
  private final RegionNodeMatcher regionNodeMatcher;

  protected SonarPluginJavaParserChanger(
      final RuleIssues ruleIssues, final Class<? extends Node> nodeType) {
    this.ruleIssues = Objects.requireNonNull(ruleIssues);
    this.nodeType = Objects.requireNonNull(nodeType);
    this.regionNodeMatcher = RegionNodeMatcher.EXACT_MATCH;
  }

  @Override
  public List<CodemodChange> visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<Issue> issues = ruleIssues.getResultsByPath(context.path());

    // small shortcut to avoid always executing the expensive findAll
    if (issues.isEmpty()) {
      return List.of();
    }

    List<? extends Node> allNodes = cu.findAll(nodeType);

    /*
     * We have an interesting scenario we have to handle whereby we could accidentally feed two results that should be
     * applied only once. Consider this real-world example where a SARIF says we have 1 problem on line 101, column 3:
     *
     * 100. public void foo() {
     * 101.   Runtime.getRuntime().exec(...);
     * 102. }
     *
     * There are actually 2 different JavaParser nodes that match the position reported in SARIF:
     * - getRuntime()
     * - exec(...)
     *
     * If we apply the change to both nodes, we'll end up with a broken AST. So we need to keep track of the nodes we've
     * already applied changes to, and skip them if we encounter them again. Deciding which of the nodes we want to act
     * on is unfortunately a job for the subclass -- they should just "return false" if the event didn't make sense, but
     * we should invest into a general solution if this doesn't scale.
     */
    List<CodemodChange> codemodChanges = new ArrayList<>();
    for (Issue issue : issues) {
      for (Node node : allNodes) {
        Position start =
            new Position(
                issue.getTextRange().getStartLine(), issue.getTextRange().getStartOffset());
        Position end =
            new Position(issue.getTextRange().getEndLine(), issue.getTextRange().getEndOffset());
        SourceCodeRegion region = new SourceCodeRegion(start, end);
        if (!nodeType.isAssignableFrom(node.getClass())) {
          continue;
        }
        if (context.lineIncludesExcludes().matches(region.start().line())) {
          if (node.getRange().isPresent()) {
            Range range = node.getRange().get();
            if (regionNodeMatcher.matches(region, range)) {
              boolean changeSuccessful = onIssueFound(context, cu, (T) node, issue);
              if (changeSuccessful) {
                codemodChanges.add(
                    CodemodChange.from(region.start().line(), dependenciesRequired()));
              }
            }
          }
        }
      }
    }
    return codemodChanges;
  }

  /**
   * Creates a visitor for the given context and locations.
   *
   * @param context the context of this files transformation
   * @param cu the parsed model of the file being transformed
   * @param node the node to act on
   * @param issue the given Sonar issue to act on
   * @return true, if the change was made, false otherwise
   */
  public abstract boolean onIssueFound(
      CodemodInvocationContext context, CompilationUnit cu, T node, Issue issue);
}
