package io.codemodder.plugins.maven.operator;

class CheckDependencyPresent extends AbstractCommand {
  private static final CheckDependencyPresent INSTANCE = new CheckDependencyPresent();

  private CheckDependencyPresent() {
    // Private constructor to prevent instantiation
  }

  public static CheckDependencyPresent getInstance() {
    return INSTANCE;
  }

  @Override
  public boolean execute(ProjectModel pm) {
    /** CheckDependencyPresentJ requires a Dependency to be Present */
    if (pm.getDependency() == null)
      throw new MissingDependencyException("Dependency must be present for modify");

    return false;
  }
}
