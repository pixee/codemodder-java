package io.codemodder.plugins.maven.operator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractQueryCommand extends AbstractCommand {

  public static final String DEPENDENCY_TREE_MOJO_REFERENCE =
      "org.apache.maven.plugins:maven-dependency-plugin:3.3.0:tree";

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractQueryCommand.class);

  private File getOutputPath(File pomFilePath) {
    File basePath = pomFilePath.getParentFile();

    String outputBasename = String.format("output-%08X.txt", pomFilePath.hashCode());

    File outputPath = new File(basePath, outputBasename);

    return outputPath;
  }

  protected File getPomFilePath(POMDocument d) throws URISyntaxException {
    Path pomPath = Paths.get(d.getPomPath().toURI());
    return pomPath.toFile();
  }

  protected abstract void extractDependencyTree(File outputPath, File pomFilePath, ProjectModel c);

  protected Collection<Dependency> result = null;

  public Collection<Dependency> getResult() {
    return result;
  }

  public void setResult(Collection<Dependency> result) {
    this.result = result;
  }

  @Override
  public boolean execute(ProjectModel pm) throws URISyntaxException, IOException {
    File pomFilePath = getPomFilePath(pm.getPomFile());
    File outputPath = getOutputPath(pomFilePath);

    if (outputPath.exists()) {
      outputPath.delete();
    }

    try {
      extractDependencyTree(outputPath, pomFilePath, pm);
    } catch (InvalidContextException e) {
      return false;
    }

    result = extractDependencies(outputPath).values();

    return true;
  }

  protected Map<String, Dependency> extractDependencies(File outputPath) throws IOException {
    Map<String, Dependency> dependencyMap = new HashMap<>();
    try (BufferedReader reader = new BufferedReader(new FileReader(outputPath))) {
      String line;
      boolean skipFirstLine = true;
      while ((line = reader.readLine()) != null) {
        if (skipFirstLine) {
          skipFirstLine = false;
          continue;
        }
        String trimmedLine = trimSpecialChars(line);
        String[] elements = trimmedLine.split(":");

        if (elements.length >= 5) {
          String groupId = elements[0];
          String artifactId = elements[1];
          String packaging = elements[2];
          String version = elements[3];
          String scope = elements[4];

          Dependency dependency =
              new Dependency(groupId, artifactId, version, null, packaging, scope);
          dependencyMap.put(line, dependency);
        }
      }
    }
    return dependencyMap;
  }

  private String trimSpecialChars(String input) {
    char[] specialChars = "+-|\\ ".toCharArray();
    int start = 0;
    int end = input.length();

    while (start < end && isSpecialChar(input.charAt(start))) {
      start++;
    }

    while (end > start && isSpecialChar(input.charAt(end - 1))) {
      end--;
    }

    if (start > 0 || end < input.length()) {
      return input.substring(start, end);
    }

    return input;
  }

  private boolean isSpecialChar(char c) {
    char[] specialChars = "+-|\\ ".toCharArray();
    for (char specialChar : specialChars) {
      if (c == specialChar) {
        return true;
      }
    }
    return false;
  }

  protected InvocationRequest buildInvocationRequest(
      File outputPath, File pomFilePath, ProjectModel c) {
    Properties props = new Properties(System.getProperties());
    props.setProperty("outputFile", outputPath.getAbsolutePath());

    String localRepositoryPath = getLocalRepositoryPath(c).getAbsolutePath();
    props.setProperty("maven.repo.local", localRepositoryPath);

    InvocationRequest request = new DefaultInvocationRequest();
    findMaven(request);

    request.setPomFile(pomFilePath);
    request.setShellEnvironmentInherited(true);
    request.setNoTransferProgress(true);
    request.setBatchMode(true);
    request.setRecursive(false);
    request.setProfiles(Arrays.asList(c.getActiveProfiles().toArray(new String[0])));
    request.setDebug(true);
    request.setOffline(c.isOffline());
    request.setProperties(props);

    List<String> goals = new ArrayList<>();
    goals.add(AbstractQueryCommand.DEPENDENCY_TREE_MOJO_REFERENCE);
    request.setGoals(goals);

    return request;
  }

  private void findMaven(InvocationRequest invocationRequest) {
    /*
     * Step 1: Locate Maven Home
     */
    String m2homeEnvVar = System.getenv("M2_HOME");

    if (m2homeEnvVar != null) {
      File m2HomeDir = new File(m2homeEnvVar);

      if (m2HomeDir.isDirectory()) {
        invocationRequest.setMavenHome(m2HomeDir);
      }
    }

    /** Step 1.1: Try to guess if that's the case */
    if (invocationRequest.getMavenHome() == null) {
      File inferredHome = new File(SystemUtils.getUserHome(), ".m2");

      if (!(inferredHome.exists() && inferredHome.isDirectory())) {
        LOGGER.warn(
            "Inferred User Home - which does not exist or not a directory: {}", inferredHome);
      }

      invocationRequest.setMavenHome(inferredHome);
    }

    /** Step 2: Find Maven Executable given the operating system and PATH variable contents */
    List<String> possibleExecutables = Arrays.asList("mvn", "mvnw");

    File foundExecutable =
        possibleExecutables.stream()
            .map(Util::which)
            .filter(exec -> exec != null)
            .findFirst()
            .orElse(null);

    if (foundExecutable != null) {
      invocationRequest.setMavenExecutable(foundExecutable);
    } else {
      throw new IllegalStateException("Missing Maven Home / Executable");
    }
  }

  @Override
  public boolean postProcess(ProjectModel c) {
    return false;
  }
}
