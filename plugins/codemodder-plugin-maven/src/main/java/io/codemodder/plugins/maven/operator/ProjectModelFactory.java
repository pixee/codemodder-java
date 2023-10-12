package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import org.dom4j.DocumentException;

/** Builder Object for ProjectModel instances */
public class ProjectModelFactory {
  private POMDocument pomFile;
  private List<POMDocument> parentPomFiles;
  private Dependency dependency;
  private boolean skipIfNewer;
  private boolean useProperties;
  private Set<String> activeProfiles;
  private boolean overrideIfAlreadyExists;
  private QueryType queryType;
  private File repositoryPath;
  private boolean offline;

  private ProjectModelFactory() {
    parentPomFiles = new ArrayList<>();
    activeProfiles = new HashSet<>();
    queryType = QueryType.NONE;
  }

  /**
   * Fluent Setter
   *
   * @param pomFile POM File
   */
  public ProjectModelFactory withPomFile(POMDocument pomFile) {
    this.pomFile = pomFile;
    return this;
  }

  /**
   * Fluent Setter
   *
   * @param parentPomFiles Parent POM Files
   */
  public ProjectModelFactory withParentPomFiles(Collection<POMDocument> parentPomFiles) {
    this.parentPomFiles =
        new ArrayList<>(
            parentPomFiles.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    return this;
  }

  /**
   * Fluent Setter
   *
   * @param dep dependency
   */
  public ProjectModelFactory withDependency(Dependency dep) {
    this.dependency = dep;
    return this;
  }

  /** Fluent Setter */
  public ProjectModelFactory withSkipIfNewer(boolean skipIfNewer) {
    this.skipIfNewer = skipIfNewer;
    return this;
  }

  /** Fluent Setter */
  public ProjectModelFactory withUseProperties(boolean useProperties) {
    this.useProperties = useProperties;
    return this;
  }

  /** Fluent Setter */
  public ProjectModelFactory withActiveProfiles(String... activeProfiles) {
    this.activeProfiles = new HashSet<>(Arrays.asList(activeProfiles));
    return this;
  }

  /** Fluent Setter */
  public ProjectModelFactory withOverrideIfAlreadyExists(boolean overrideIfAlreadyExists) {
    this.overrideIfAlreadyExists = overrideIfAlreadyExists;
    return this;
  }

  /**
   * Fluent Setter
   *
   * @param queryType query type
   */
  public ProjectModelFactory withQueryType(QueryType queryType) {
    this.queryType = queryType;
    return this;
  }

  /**
   * Fluent Setter
   *
   * @param repositoryPath Repository Path
   */
  public ProjectModelFactory withRepositoryPath(File repositoryPath) {
    this.repositoryPath = repositoryPath;
    return this;
  }

  /**
   * Fluent Setter
   *
   * @param offline Offline
   */
  public ProjectModelFactory withOffline(boolean offline) {
    this.offline = offline;
    return this;
  }

  private static ProjectModelFactory create() {
    return new ProjectModelFactory();
  }

  /** Mostly Delegates to POMDocumentFactory */
  static ProjectModelFactory load(InputStream is)
      throws DocumentException, IOException, URISyntaxException {
    POMDocument pomDocument = POMDocumentFactory.load(is);
    return ProjectModelFactory.create().withPomFile(pomDocument);
  }

  /** Mostly Delegates to POMDocumentFactory */
  static ProjectModelFactory load(File f) throws Exception {
    URL fileUrl = f.toURI().toURL();
    return load(fileUrl);
  }

  /** Mostly Delegates to POMDocumentFactory */
  static ProjectModelFactory load(URL url)
      throws DocumentException, IOException, URISyntaxException {
    POMDocument pomFile = POMDocumentFactory.load(url);
    return ProjectModelFactory.create().withPomFile(pomFile);
  }

  /** Mostly Delegates to POMDocumentFactory */
  static ProjectModelFactory loadFor(POMDocument pomFile, Collection<POMDocument> parentPomFiles) {
    List<POMDocument> parentPomFilesList = new ArrayList<>(parentPomFiles);
    ProjectModelFactory pmf = ProjectModelFactory.create();
    return ProjectModelFactory.create().withPomFile(pomFile).withParentPomFiles(parentPomFilesList);
  }

  /** Fluent Setter */
  public ProjectModel build() {
    return new ProjectModel(
        pomFile,
        parentPomFiles,
        dependency,
        skipIfNewer,
        useProperties,
        activeProfiles,
        overrideIfAlreadyExists,
        queryType,
        repositoryPath,
        null,
        offline);
  }
}
