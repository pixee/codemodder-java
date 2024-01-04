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

  private final NodeCollector nodeCollector;

  protected SonarPluginJavaParserChanger(
      final RuleIssues ruleIssues,
      final Class<? extends Node> nodeType,
      final RegionNodeMatcher regionNodeMatcher,
      final NodeCollector nodeCollector) {
    this.ruleIssues = Objects.requireNonNull(ruleIssues);
    this.nodeType = Objects.requireNonNull(nodeType);
    this.regionNodeMatcher = regionNodeMatcher;
    this.nodeCollector = nodeCollector;
  }

  protected SonarPluginJavaParserChanger(
      final RuleIssues ruleIssues, final Class<? extends Node> nodeType) {
    this(ruleIssues, nodeType, RegionNodeMatcher.MATCHES_START, NodeCollector.ALL_FROM_TYPE);
  }

  protected SonarPluginJavaParserChanger(
      final RuleIssues ruleIssues,
      final Class<? extends Node> nodeType,
      final RegionNodeMatcher regionNodeMatcher,
      final CodemodReporterStrategy codemodReporterStrategy) {
    super(codemodReporterStrategy);
    this.ruleIssues = Objects.requireNonNull(ruleIssues);
    this.nodeType = Objects.requireNonNull(nodeType);
    this.regionNodeMatcher = regionNodeMatcher;
    this.nodeCollector = NodeCollector.ALL_FROM_TYPE;
  }

  @Override
  public List<CodemodChange> visit(
      final CodemodInvocationContext context, final CompilationUnit cu) {
    List<Issue> issues = ruleIssues.getResultsByPath(context.path());

    // small shortcut to avoid always executing the expensive findAll
    if (issues == null || issues.isEmpty()) {
      return List.of();
    }
    final List<? extends Node> allNodes = nodeCollector.collectNodes(cu, nodeType);

    List<CodemodChange> codemodChanges = new ArrayList<>();
    for (Issue issue : issues) {
      for (Node node : allNodes) {
        Position start =
            new Position(
                issue.getTextRange().getStartLine(), issue.getTextRange().getStartOffset() + 1);
        Position end =
            new Position(
                issue.getTextRange().getEndLine(), issue.getTextRange().getEndOffset() + 1);
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

  @Override
  public boolean shouldRun() {
    return ruleIssues.hasResults();
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
