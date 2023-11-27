package io.codemodder.plugins.maven.operator;

import com.github.zafarkhaja.semver.Version;
import io.codemodder.DependencyGAV;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;

/** Facade for the POM Operator, providing methods for modifying and querying POM files. */
public class POMOperator {
  private final POMScanner pomScanner;

  public POMOperator(final Path pomFile, final Path projectDir) {
    this.pomScanner = new POMScanner(pomFile, projectDir);
  }

  public POMScanner getPomScanner() {
    return pomScanner;
  }

  /**
   * Modifies and retrieves the ProjectModel with a new dependency.
   *
   * @param newDependencyGAV The new DependencyGAV to add to the POM.
   * @return The modified ProjectModel, or null if the modification was unsuccessful.
   * @throws XMLStreamException If an error occurs during XML stream processing.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   * @throws IOException If an I/O error occurs.
   * @throws DocumentException If an error occurs while parsing the document.
   */
  public ProjectModel addDependency(final DependencyGAV newDependencyGAV)
      throws XMLStreamException, URISyntaxException, IOException, DocumentException {
    final Dependency newDependency = new Dependency(newDependencyGAV);
    final ProjectModel projectModel =
        pomScanner
            .scanFrom()
            .withDependency(newDependency)
            .withSkipIfNewer(true)
            .withUseProperties(true)
            .withRepositoryPath(FileUtils.createTempDirectoryWithPermissions())
            .build();

    return modify(projectModel) ? projectModel : null;
  }

  /**
   * Retrieves all found dependencies in the POM.
   *
   * @return A collection of DependencyGAV objects representing the found dependencies.
   * @throws DocumentException If an error occurs while parsing the document.
   * @throws IOException If an I/O error occurs.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   * @throws XMLStreamException If an error occurs during XML stream processing.
   */
  @NotNull
  public Collection<DependencyGAV> getAllFoundDependencies()
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {

    final ProjectModel originalProjectModel =
        pomScanner
            .scanFrom()
            .withSafeQueryType()
            .withRepositoryPath(FileUtils.createTempDirectoryWithPermissions())
            .build();

    final Collection<Dependency> foundDependencies = queryDependency(originalProjectModel);

    return foundDependencies.stream()
        .map(
            dependency ->
                DependencyGAV.createDefault(
                    dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion()))
        .toList();
  }

  /**
   * Bump a Dependency Version on a POM.
   *
   * @param projectModel Project Model (Context) class
   * @return true if the modification was successful; otherwise, false.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   * @throws IOException If an I/O error occurs.
   * @throws XMLStreamException If an error occurs while handling XML streams.
   */
  static boolean modify(ProjectModel projectModel)
      throws URISyntaxException, IOException, XMLStreamException {
    return CommandChain.modifyDependency().execute(projectModel);
  }

  /**
   * Method to insert only a dependency onto a POM
   *
   * @param projectModel Project Model (Context) class
   * @return true if the modification was successful; otherwise, false.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   * @throws IOException If an I/O error occurs.
   * @throws XMLStreamException If an error occurs while handling XML streams.
   */
  static boolean insertOnly(ProjectModel projectModel)
      throws URISyntaxException, IOException, XMLStreamException {
    return CommandChain.insertDependency().execute(projectModel);
  }

  /**
   * Method to update only a dependency (its version) onto a POM
   *
   * @param projectModel Project Model (Context) class
   * @return true if the modification was successful; otherwise, false.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   * @throws IOException If an I/O error occurs.
   * @throws XMLStreamException If an error occurs while handling XML streams.
   */
  static boolean updateOnly(ProjectModel projectModel)
      throws URISyntaxException, IOException, XMLStreamException {
    return CommandChain.updateDependency().execute(projectModel);
  }

  /**
   * Public API - Query for all the artifacts referenced inside a POM File.
   *
   * @param projectModel Project Model (Context) Class
   * @return a collection of Dependency objects representing the artifacts referenced in the POM.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   * @throws IOException If an I/O error occurs.
   * @throws XMLStreamException If an error occurs while handling XML streams.
   */
  static Collection<Dependency> queryDependency(ProjectModel projectModel)
      throws URISyntaxException, IOException, XMLStreamException {
    return queryDependency(projectModel, Collections.emptyList());
  }

  /**
   * Public API - Query for all the versions mentioned inside a POM File.
   *
   * @param projectModel Project Model (Context) Class
   * @return an optional VersionQueryResponse object containing source and target versions, if
   *     found.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   * @throws IOException If an I/O error occurs.
   * @throws XMLStreamException If an error occurs while handling XML streams.
   */
  static Optional<VersionQueryResponse> queryVersions(ProjectModel projectModel)
      throws URISyntaxException, IOException, XMLStreamException {
    Set<VersionDefinition> queryVersionResult =
        queryVersions(projectModel, Collections.emptyList());

    /*
     * Likely Source / Target
     */
    if (queryVersionResult.size() == 2) {
      /*
       * but if there's `release` we`ll throw an exception
       */
      if (queryVersionResult.stream().anyMatch(it -> it.getKind() == Kind.RELEASE)) {
        throw new IllegalStateException(
            "Unexpected queryVersionResult Combination: " + queryVersionResult);
      }

      VersionDefinition queryVersionSource =
          queryVersionResult.stream()
              .filter(it -> it.getKind() == Kind.SOURCE)
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Missing source version"));

      VersionDefinition queryVersionTarget =
          queryVersionResult.stream()
              .filter(it -> it.getKind() == Kind.TARGET)
              .findFirst()
              .orElseThrow(() -> new IllegalStateException("Missing target version"));

      Version mappedSourceVersion = mapVersion(queryVersionSource.getValue());
      Version mappedTargetVersion = mapVersion(queryVersionTarget.getValue());

      return Optional.of(new VersionQueryResponse(mappedSourceVersion, mappedTargetVersion));
    }

    /** Could be either source, target or release - we pick the value anyway */
    if (queryVersionResult.size() == 1) {
      List<VersionDefinition> queryVersionResultList =
          queryVersionResult != null && !queryVersionResult.isEmpty()
              ? queryVersionResult.stream().collect(Collectors.toList())
              : Collections.emptyList();
      Version mappedVersion = mapVersion(queryVersionResultList.get(0).getValue());

      VersionQueryResponse returnValue = new VersionQueryResponse(mappedVersion, mappedVersion);

      return Optional.of(returnValue);
    }

    return Optional.empty();
  }

  /**
   * Given a version string, formats and returns it as a semantic version object.
   *
   * <p>Versions starting with "1." are appended with ".0".
   *
   * <p>Other versions are appended with ".0.0".
   *
   * @param version The version string to map.
   * @return the mapped semantic version.
   */
  private static Version mapVersion(String version) {
    String fixedVersion = version + (version.startsWith("1.") ? ".0" : ".0.0");
    return Version.valueOf(fixedVersion);
  }

  /**
   * Internal Use (package-wide) - Query for dependencies mentioned on a POM.
   *
   * @param projectModel Project Model (Context) class
   * @param commandList do not use (required for tests)
   */
  static Collection<Dependency> queryDependency(
      ProjectModel projectModel, List<Command> commandList)
      throws URISyntaxException, IOException, XMLStreamException {
    CommandChain chain = CommandChain.createForDependencyQuery(projectModel.getQueryType());

    executeChain(commandList, chain, projectModel);

    AbstractQueryCommand lastCommand = null;
    for (int i = chain.getCommandList().size() - 1; i >= 0; i--) {
      if (chain.getCommandList().get(i) instanceof AbstractQueryCommand) {
        lastCommand = (AbstractQueryCommand) chain.getCommandList().get(i);
        if (lastCommand.getResult() != null) {
          break;
        }
      }
    }

    if (lastCommand == null) {
      return Collections.emptyList();
    }

    return lastCommand.getResult();
  }

  /**
   * Internal Use (package-wide) - Query for versions mentioned on a POM.
   *
   * @param projectModel Project Model (Context) class
   * @param commandList do not use (required for tests)
   */
  static Set<VersionDefinition> queryVersions(ProjectModel projectModel, List<Command> commandList)
      throws URISyntaxException, IOException, XMLStreamException {
    CommandChain chain = CommandChain.createForVersionQuery(projectModel.getQueryType());

    executeChain(commandList, chain, projectModel);

    AbstractVersionCommand lastCommand = null;
    for (int i = chain.getCommandList().size() - 1; i >= 0; i--) {
      if (chain.getCommandList().get(i) instanceof AbstractVersionCommand) {
        lastCommand = (AbstractVersionCommand) chain.getCommandList().get(i);
        if (lastCommand.result != null && !lastCommand.result.isEmpty()) {
          break;
        }
      }
    }

    if (lastCommand == null) {
      return Collections.emptySet();
    }

    return lastCommand.result;
  }

  private static void executeChain(
      List<Command> commandList, CommandChain chain, ProjectModel projectModel)
      throws URISyntaxException, IOException, XMLStreamException {
    if (!commandList.isEmpty()) {
      chain.getCommandList().clear();
      chain.getCommandList().addAll(commandList);
    }

    chain.execute(projectModel);
  }
}
