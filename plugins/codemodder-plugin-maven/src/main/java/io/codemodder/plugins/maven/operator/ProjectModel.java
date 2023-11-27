package io.codemodder.plugins.maven.operator;

import java.nio.file.Path;
import java.util.*;
import org.dom4j.Element;
import org.dom4j.Node;

/** ProjectModel represents the input parameters for the chain */
public class ProjectModel {
  private POMDocument pomFile;
  private List<POMDocument> parentPomFiles;
  private Dependency dependency;
  private boolean skipIfNewer;
  private boolean useProperties;
  private Set<String> activeProfiles;
  private boolean overrideIfAlreadyExists;
  private QueryType queryType;
  private Path repositoryPath;
  private String finishedByClass;
  private boolean modifiedByCommand;

  /**
   * Constructs a new ProjectModel instance with the specified parameters.
   *
   * @param pomFile The POMDocument representing the main POM file.
   * @param parentPomFiles A list of POMDocuments representing parent POM files.
   * @param dependency The Dependency object to operate on.
   * @param skipIfNewer Whether to skip the operation if the dependency is newer.
   * @param useProperties Whether to use properties during the operation.
   * @param activeProfiles A set of active profiles to consider during property resolution.
   * @param overrideIfAlreadyExists Whether to override the dependency if it already exists.
   * @param queryType The type of query operation to perform.
   * @param repositoryPath The path to the repository.
   * @param finishedByClass The name of the class that finished the operation.
   */
  public ProjectModel(
      POMDocument pomFile,
      List<POMDocument> parentPomFiles,
      Dependency dependency,
      boolean skipIfNewer,
      boolean useProperties,
      Set<String> activeProfiles,
      boolean overrideIfAlreadyExists,
      QueryType queryType,
      Path repositoryPath,
      String finishedByClass) {
    this.pomFile = pomFile;
    this.parentPomFiles = parentPomFiles != null ? parentPomFiles : Collections.emptyList();
    this.dependency = dependency;
    this.skipIfNewer = skipIfNewer;
    this.useProperties = useProperties;
    this.activeProfiles = activeProfiles;
    this.overrideIfAlreadyExists = overrideIfAlreadyExists;
    this.queryType = queryType != null ? queryType : QueryType.NONE;
    this.repositoryPath = repositoryPath;
    this.finishedByClass = finishedByClass;
    this.modifiedByCommand = false;
  }

  /**
   * Returns a map of properties defined on the root of the given POMDocument.
   *
   * @param pomFile The POMDocument to extract properties from.
   * @return A map of property names and their values.
   */
  public static Map<String, String> propertiesDefinedOnPomDocument(POMDocument pomFile) {
    Map<String, String> rootProperties = new HashMap<>();
    List<Element> propertyElements =
        pomFile.getPomDocument().getRootElement().elements("properties");
    for (Element element : propertyElements) {
      List<Element> elements = element.elements();
      for (Element propertyElement : elements) {
        rootProperties.put(propertyElement.getName(), propertyElement.getText());
      }
    }
    return rootProperties;
  }

  private Map<String, String> getPropertiesFromProfile(String profileName, POMDocument pomFile) {
    String expression =
        "/m:project/m:profiles/m:profile[./m:id[text()='" + profileName + "']]/m:properties";
    List<Node> propertiesElements = Util.selectXPathNodes(pomFile.getPomDocument(), expression);

    Map<String, String> newPropertiesToAppend = new HashMap<>();
    for (Node element : propertiesElements) {
      if (element instanceof Element) {
        List<Element> elements = ((Element) element).elements();
        for (Element propertyElement : elements) {
          newPropertiesToAppend.put(propertyElement.getName(), propertyElement.getText());
        }
      }
    }

    return newPropertiesToAppend;
  }

  /**
   * Returns a map of properties defined in various POM files based on their names.
   *
   * @return A map where keys are property names, and values are lists of pairs containing the
   *     property value and the corresponding POMDocument.
   */
  public Map<String, List<Pair<String, POMDocument>>> propertiesDefinedByFile() {
    Map<String, List<Pair<String, POMDocument>>> result = new LinkedHashMap<>();
    List<POMDocument> allPomFiles = allPomFiles();

    for (POMDocument pomFile : allPomFiles) {
      Map<String, String> rootProperties = propertiesDefinedOnPomDocument(pomFile);
      Map<String, String> tempProperties = new LinkedHashMap<>(rootProperties);

      List<String> activatedProfiles = new ArrayList<>();
      for (String profile : activeProfiles) {
        if (!profile.startsWith("!")) {
          activatedProfiles.add(profile);
        }
      }

      List<Map<String, String>> newPropertiesFromProfiles = new ArrayList<>();
      for (String profileName : activatedProfiles) {
        newPropertiesFromProfiles.add(getPropertiesFromProfile(profileName, pomFile));
      }

      for (Map<String, String> properties : newPropertiesFromProfiles) {
        tempProperties.putAll(properties);
      }

      for (Map.Entry<String, String> entry : tempProperties.entrySet()) {
        String key = entry.getKey();

        if (!result.containsKey(key)) {
          result.put(key, new ArrayList<>());
        }

        List<Pair<String, POMDocument>> definitionList = result.get(key);
        definitionList.add(new Pair<>(entry.getValue(), pomFile));
      }
    }

    return result;
  }

  /**
   * Returns a map of resolved properties from the POM files in the context of active profiles.
   *
   * @return A map of property names and their resolved values.
   */
  public Map<String, String> resolvedProperties() {
    Map<String, String> result = new LinkedHashMap<>();
    List<POMDocument> allPomFiles = allPomFiles();
    Collections.reverse(allPomFiles); // parent first, children later - thats why its reversed

    for (POMDocument pomFile : allPomFiles) {
      Map<String, String> rootProperties = propertiesDefinedOnPomDocument(pomFile);
      result.putAll(rootProperties);

      List<String> activatedProfiles = new ArrayList<>();
      for (String profile : activeProfiles) {
        if (!profile.startsWith("!")) {
          activatedProfiles.add(profile);
        }
      }

      List<Map<String, String>> newPropertiesFromProfiles = new ArrayList<>();
      for (String profileName : activatedProfiles) {
        newPropertiesFromProfiles.add(getPropertiesFromProfile(profileName, pomFile));
      }

      for (Map<String, String> properties : newPropertiesFromProfiles) {
        result.putAll(properties);
      }
    }

    return Collections.unmodifiableMap(result);
  }

  /**
   * Returns a list of all POM files involved in the context.
   *
   * @return A list of POMDocument instances representing all relevant POM files.
   */
  public List<POMDocument> allPomFiles() {
    List<POMDocument> allFiles = new ArrayList<>();
    allFiles.add(pomFile);
    allFiles.addAll(parentPomFiles);
    return allFiles;
  }

  public POMDocument getPomFile() {
    return pomFile;
  }

  public void setPomFile(POMDocument pomFile) {
    this.pomFile = pomFile;
  }

  public List<POMDocument> getParentPomFiles() {
    return parentPomFiles;
  }

  public void setParentPomFiles(List<POMDocument> parentPomFiles) {
    this.parentPomFiles = parentPomFiles;
  }

  public Dependency getDependency() {
    return dependency;
  }

  public void setDependency(Dependency dependency) {
    this.dependency = dependency;
  }

  public boolean isSkipIfNewer() {
    return skipIfNewer;
  }

  public void setSkipIfNewer(boolean skipIfNewer) {
    this.skipIfNewer = skipIfNewer;
  }

  public boolean isUseProperties() {
    return useProperties;
  }

  public void setUseProperties(boolean useProperties) {
    this.useProperties = useProperties;
  }

  public Set<String> getActiveProfiles() {
    return activeProfiles;
  }

  public void setActiveProfiles(Set<String> activeProfiles) {
    this.activeProfiles = activeProfiles;
  }

  public boolean isOverrideIfAlreadyExists() {
    return overrideIfAlreadyExists;
  }

  public void setOverrideIfAlreadyExists(boolean overrideIfAlreadyExists) {
    this.overrideIfAlreadyExists = overrideIfAlreadyExists;
  }

  public QueryType getQueryType() {
    return queryType;
  }

  public void setQueryType(QueryType queryType) {
    this.queryType = queryType;
  }

  public Path getRepositoryPath() {
    return repositoryPath;
  }

  public void setRepositoryPath(Path repositoryPath) {
    this.repositoryPath = repositoryPath;
  }

  public String getFinishedByClass() {
    return finishedByClass;
  }

  public void setFinishedByClass(String finishedByClass) {
    this.finishedByClass = finishedByClass;
  }

  public boolean isModifiedByCommand() {
    return modifiedByCommand;
  }

  public void setModifiedByCommand(boolean modifiedByCommand) {
    this.modifiedByCommand = modifiedByCommand;
  }
}
