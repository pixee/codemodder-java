package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;
import org.dom4j.DocumentException;

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

  public ProjectModelFactory withPomFile(POMDocument pomFile) {
    this.pomFile = pomFile;
    return this;
  }

  public ProjectModelFactory withParentPomFiles(Collection<POMDocument> parentPomFiles) {
    this.parentPomFiles =
        new ArrayList<>(
            parentPomFiles.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    return this;
  }

  public ProjectModelFactory withDependency(Dependency dep) {
    this.dependency = dep;
    return this;
  }

  public ProjectModelFactory withSkipIfNewer(boolean skipIfNewer) {
    this.skipIfNewer = skipIfNewer;
    return this;
  }

  public ProjectModelFactory withUseProperties(boolean useProperties) {
    this.useProperties = useProperties;
    return this;
  }

  public ProjectModelFactory withActiveProfiles(String... activeProfiles) {
    this.activeProfiles = new HashSet<>(Arrays.asList(activeProfiles));
    return this;
  }

  public ProjectModelFactory withOverrideIfAlreadyExists(boolean overrideIfAlreadyExists) {
    this.overrideIfAlreadyExists = overrideIfAlreadyExists;
    return this;
  }

  public ProjectModelFactory withQueryType(QueryType queryType) {
    this.queryType = queryType;
    return this;
  }

  public ProjectModelFactory withRepositoryPath(File repositoryPath) {
    this.repositoryPath = repositoryPath;
    return this;
  }

  public ProjectModelFactory withOffline(boolean offline) {
    this.offline = offline;
    return this;
  }

  public static ProjectModelFactory create() {
    return new ProjectModelFactory();
  }

  public static ProjectModelFactory load(InputStream is)
      throws DocumentException, IOException, URISyntaxException {
    POMDocument pomDocument = POMDocumentFactory.load(is);
    return ProjectModelFactory.create().withPomFile(pomDocument);
  }

  public static ProjectModelFactory load(File f) throws Exception {
    URL fileUrl = f.toURI().toURL();
    return load(fileUrl);
  }

  public static ProjectModelFactory load(URL url)
      throws DocumentException, IOException, URISyntaxException {
    POMDocument pomFile = POMDocumentFactory.load(url);
    return ProjectModelFactory.create().withPomFile(pomFile);
  }

  public static ProjectModelFactory loadFor(
      POMDocument pomFile, Collection<POMDocument> parentPomFiles) {
    List<POMDocument> parentPomFilesList = new ArrayList<>(parentPomFiles);
    ProjectModelFactory pmf = ProjectModelFactory.create();
    return ProjectModelFactory.create().withPomFile(pomFile).withParentPomFiles(parentPomFilesList);
  }

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
