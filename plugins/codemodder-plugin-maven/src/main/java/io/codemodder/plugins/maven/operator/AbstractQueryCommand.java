package io.codemodder.plugins.maven.operator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common Base Class - Meant to be used by Simple Queries using either Invoker and/or Embedder, thus
 * relying on dependency:tree mojo outputting into a text file - which might be cached.
 */
abstract class AbstractQueryCommand extends AbstractCommand {

  protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractQueryCommand.class);

  /**
   * Generates a temporary file path used to store the output of the
   *
   * <pre>dependency:tree</pre>
   *
   * mojo
   *
   * @param pomFilePath POM Original File Path
   */
  private File getOutputPath(File pomFilePath) {
    File basePath = pomFilePath.getParentFile();

    String outputBasename = String.format("output-%08X.txt", pomFilePath.hashCode());

    File outputPath = new File(basePath, outputBasename);

    return outputPath;
  }

  /**
   * Given a POM URI, returns a File Object
   *
   * @param d POMDocument
   */
  private File getPomFilePath(POMDocument d) throws URISyntaxException {
    Path pomPath = Paths.get(d.getPomPath().toURI());
    return pomPath.toFile();
  }

  /**
   * Abstract Method to extract dependencies
   *
   * @param outputPath Output Path to where to store the content
   * @param pomFilePath Input Pom Path
   * @param c Project Model
   */
  protected abstract void extractDependencyTree(File outputPath, File pomFilePath, ProjectModel c);

  /**
   * Internal Holder Variable
   *
   * <p>Todo: OF COURSE IT BREAKS THE PROTOCOL
   */
  protected Collection<Dependency> result = null;

  /**
   * Retrieves the collection of dependencies resulting from the query command execution.
   *
   * @return A collection of Dependency objects representing project dependencies.
   */
  public Collection<Dependency> getResult() {
    return result;
  }

  /**
   * We declare the main logic here - details are made in the child classes for now Executes the
   * query command, extracting and processing project dependencies.
   *
   * @param pm The ProjectModel representing the project on which to execute the command.
   * @return true if the execution was successful, false otherwise.
   * @throws URISyntaxException If there is an issue with URIs in the execution.
   * @throws IOException If an IO error occurs during execution.
   */
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

  /**
   * Given a File containing the output of the dependency:tree mojo, read its contents and parse,
   * creating an array of dependencies
   *
   * <p>About the file contents: We receive something such as this, then filter it out:
   *
   * <pre>
   *     br.com.ingenieux:pom-operator:jar:0.0.1-SNAPSHOT
   *     +- xerces:xercesImpl:jar:2.12.1:compile
   *     |  \- xml-apis:xml-apis:jar:1.4.01:compile
   *     \- org.jetbrains.kotlin:kotlin-test:jar:1.5.31:test
   * </pre>
   *
   * @param outputPath file to read
   */
  private Map<String, Dependency> extractDependencies(File outputPath) throws IOException {
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

  @Override
  public boolean postProcess(ProjectModel c) {
    return false;
  }
}
