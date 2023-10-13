package io.codemodder.plugins.maven.operator;

import java.io.File;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;

/** Uses Apache Maven Invoker to implement querying the project's dependency tree. */
class QueryByInvoker extends AbstractQueryCommand {

  /**
   * Executes the "dependency:tree" Maven goal using the Apache Maven Invoker and captures the
   * project's dependency tree information.
   *
   * @param outputPath The file where the dependency tree output will be stored.
   * @param pomFilePath The path to the POM (Project Object Model) file of the project.
   * @param c The ProjectModel containing the input parameters for the operation.
   * @throws IllegalStateException If the Apache Maven Invoker returns an unexpected status code.
   * @throws RuntimeException If an error occurs during the execution of the Apache Maven Invoker.
   */
  @Override
  protected void extractDependencyTree(File outputPath, File pomFilePath, ProjectModel c) {
    DefaultInvoker invoker = new DefaultInvoker();

    InvocationRequest invocationRequest = buildInvocationRequest(outputPath, pomFilePath, c);

    InvocationResult invocationResult;
    try {
      invocationResult = invoker.execute(invocationRequest);
    } catch (MavenInvocationException e) {
      throw new RuntimeException(e);
    }

    int exitCode = invocationResult.getExitCode();

    if (exitCode != 0) {
      throw new IllegalStateException("Unexpected Status Code from Invoker: " + exitCode);
    }
  }
}
