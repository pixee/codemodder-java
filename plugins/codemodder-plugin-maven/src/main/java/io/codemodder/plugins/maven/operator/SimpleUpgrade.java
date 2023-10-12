package io.codemodder.plugins.maven.operator;

/** Represents bumping an existing dependency. */
class SimpleUpgrade extends AbstractCommand {

  private static SimpleUpgrade instance;

  private SimpleUpgrade() {}

  public static SimpleUpgrade getInstance() {
    if (instance == null) {
      instance = new SimpleUpgrade();
    }
    return instance;
  }

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
