package io.codemodder.plugins.maven.operator;

/** Represents handling dependency management for a project. */
public class SimpleDependencyManagement extends AbstractCommand {

  private static SimpleDependencyManagement instance;

  private SimpleDependencyManagement() {
    // Private constructor to prevent instantiation.
  }

  public static SimpleDependencyManagement getInstance() {
    if (instance == null) {
      instance = new SimpleDependencyManagement();
    }
    return instance;
  }

  @Override
  public boolean execute(ProjectModel pm) {
    if (pm.getDependency() == null) {
      throw new NullPointerException("Dependency must not be null.");
    }

    String lookupExpression = Util.buildLookupExpressionForDependencyManagement(pm.getDependency());

    return handleDependency(pm, lookupExpression);
  }
}
