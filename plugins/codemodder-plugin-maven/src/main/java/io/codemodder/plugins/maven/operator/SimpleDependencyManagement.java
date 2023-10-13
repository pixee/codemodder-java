package io.codemodder.plugins.maven.operator;

/** Represents handling dependency management for a project. */
class SimpleDependencyManagement extends AbstractCommand {

  private static SimpleDependencyManagement instance;

  private SimpleDependencyManagement() {}

  /**
   * Gets the singleton instance of SimpleDependencyManagement.
   *
   * @return The singleton instance of SimpleDependencyManagement.
   */
  public static SimpleDependencyManagement getInstance() {
    if (instance == null) {
      instance = new SimpleDependencyManagement();
    }
    return instance;
  }

  /**
   * Executes the dependency management for a project based on the provided ProjectModel.
   *
   * @param pm The ProjectModel containing the configuration and settings for dependency management.
   * @return `true` if the dependency management is successful, `false` otherwise.
   */
  @Override
  public boolean execute(ProjectModel pm) {
    if (pm.getDependency() == null) {
      throw new NullPointerException("Dependency must not be null.");
    }

    String lookupExpression = Util.buildLookupExpressionForDependencyManagement(pm.getDependency());

    return handleDependency(pm, lookupExpression);
  }
}
