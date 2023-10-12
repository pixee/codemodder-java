package io.codemodder.plugins.maven.operator;

import java.io.File;

class CheckLocalRepositoryDirCommand {

  static class CheckParentDirCommand extends AbstractQueryCommand {

    private static final CheckParentDirCommand INSTANCE = new CheckParentDirCommand();

    private CheckParentDirCommand() {}

    public static CheckParentDirCommand getInstance() {
      return INSTANCE;
    }

    @Override
    protected void extractDependencyTree(File outputPath, File pomFilePath, ProjectModel c) {
      throw new InvalidContextException();
    }

    @Override
    public boolean execute(ProjectModel c) {
      File localRepositoryPath = getLocalRepositoryPath(c);

      if (!localRepositoryPath.exists()) {
        localRepositoryPath.mkdirs();
      }

      return false;
    }
  }
}
