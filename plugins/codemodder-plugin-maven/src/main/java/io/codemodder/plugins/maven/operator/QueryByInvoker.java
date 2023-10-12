package io.codemodder.plugins.maven.operator;

import java.io.File;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.MavenInvocationException;

class QueryByInvoker extends AbstractQueryCommand {

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
