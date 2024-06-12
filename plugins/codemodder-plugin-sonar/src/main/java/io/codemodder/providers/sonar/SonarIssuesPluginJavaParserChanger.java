package io.codemodder.providers.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.CodemodReporterStrategy;
import io.codemodder.NodeCollector;
import io.codemodder.RegionNodeMatcher;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SonarFinding;

/** Provides base functionality for making JavaParser-based changes based on Sonar results. */
public abstract class SonarIssuesPluginJavaParserChanger<T extends Node>
    extends SonarPluginJavaParserChanger<T> {

  protected SonarIssuesPluginJavaParserChanger(
      RuleIssue ruleIssue,
      Class<? extends Node> nodeType,
      RegionNodeMatcher regionNodeMatcher,
      NodeCollector nodeCollector) {
    super(ruleIssue, nodeType, regionNodeMatcher, nodeCollector);
  }

  protected SonarIssuesPluginJavaParserChanger(
      RuleIssue ruleIssue, Class<? extends Node> nodeType) {
    super(ruleIssue, nodeType);
  }

  protected SonarIssuesPluginJavaParserChanger(
      RuleIssue ruleIssue,
      Class<? extends Node> nodeType,
      RegionNodeMatcher regionNodeMatcher,
      CodemodReporterStrategy codemodReporterStrategy) {
    super(ruleIssue, nodeType, regionNodeMatcher, codemodReporterStrategy);
  }

  @Override
  protected ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final T node,
      final SonarFinding sonarFinding) {
    return onIssueFound(context, cu, node, (Issue) sonarFinding);
  }

  public abstract ChangesResult onIssueFound(
      CodemodInvocationContext context, CompilationUnit cu, T node, Issue issue);
}
