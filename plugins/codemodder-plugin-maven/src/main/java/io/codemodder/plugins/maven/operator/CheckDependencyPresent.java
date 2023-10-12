package io.codemodder.plugins.maven.operator;

/** Guard Command Singleton use to validate required parameters */
class CheckDependencyPresent extends AbstractCommand {
  private static final CheckDependencyPresent INSTANCE = new CheckDependencyPresent();

  private CheckDependencyPresent() {}

  /**
   * Gets the singleton instance of the CheckDependencyPresent command.
   *
   * @return The singleton instance of CheckDependencyPresent.
   */
  public static CheckDependencyPresent getInstance() {
    return INSTANCE;
  }

  /**
   * Executes the CheckDependencyPresent command, which ensures that a dependency is present in the
   * ProjectModel.
   *
   * @param pm ProjectModel containing project information.
   * @return false, indicating that the check was performed without errors.
   * @throws MissingDependencyException if no dependency is present in the ProjectModel.
   */
  @Override
  public boolean execute(ProjectModel pm) {
    /** CheckDependencyPresentJ requires a Dependency to be Present */
    if (pm.getDependency() == null)
      throw new MissingDependencyException("Dependency must be present for modify");

    return false;
  }
}
