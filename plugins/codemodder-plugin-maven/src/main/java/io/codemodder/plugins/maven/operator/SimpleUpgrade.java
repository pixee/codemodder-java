package io.codemodder.plugins.maven.operator;

/** Represents bumping an existing dependency. */
class SimpleUpgrade extends AbstractCommand {

  private static SimpleUpgrade instance;

  private SimpleUpgrade() {}

  /**
   * Gets the singleton instance of SimpleUpgrade.
   *
   * @return The singleton instance of SimpleUpgrade.
   */
  public static SimpleUpgrade getInstance() {
    if (instance == null) {
      instance = new SimpleUpgrade();
    }
    return instance;
  }

  /**
   * Executes the strategy for bumping an existing dependency in the Maven project.
   *
   * @param pm The ProjectModel containing the configuration and settings for the upgrade.
   * @return `true` if the upgrade is successful, `false` otherwise.
   */
  @Override
  public boolean execute(ProjectModel pm) {
    if (pm.getDependency() == null) {
      throw new NullPointerException("Dependency must not be null.");
    }

    String lookupExpressionForDependency =
        Util.buildLookupExpressionForDependency(pm.getDependency());

    return handleDependency(pm, lookupExpressionForDependency);
  }
}
