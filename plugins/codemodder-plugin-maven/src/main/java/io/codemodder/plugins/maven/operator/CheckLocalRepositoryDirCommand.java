package io.codemodder.plugins.maven.operator;

import java.io.File;

/** Commands for checking and creating the local repository directory if it doesn't exist. */
class CheckLocalRepositoryDirCommand {

  static class CheckParentDirCommand extends AbstractQueryCommand {

    private static final CheckParentDirCommand INSTANCE = new CheckParentDirCommand();

    private CheckParentDirCommand() {}

    /**
     * Gets the singleton instance of the CheckParentDirCommand.
     *
     * @return The singleton instance of CheckParentDirCommand.
     */
    public static CheckParentDirCommand getInstance() {
      return INSTANCE;
    }

    /**
     * Throws an InvalidContextException because extracting the dependency tree is not supported.
     *
     * @param outputPath Output Path to store the content (not used).
     * @param pomFilePath Input POM Path (not used).
     * @param c Project Model (not used).
     * @throws InvalidContextException Always throws an InvalidContextException.
     */
    @Override
    protected void extractDependencyTree(File outputPath, File pomFilePath, ProjectModel c) {
      throw new InvalidContextException();
    }

    /**
     * Executes the CheckParentDirCommand, which checks and creates the local repository directory
     * if it doesn't exist.
     *
     * @param c Project Model representing the project.
     * @return false, indicating that the check and directory creation were performed without
     *     errors.
     */
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
