package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.util.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.dom4j.Element;
import org.dom4j.Node;

@Getter
@Setter
public class ProjectModel {
  private POMDocument pomFile;
  private List<POMDocument> parentPomFiles;
  private Dependency dependency;
  private boolean skipIfNewer;
  private boolean useProperties;
  private Set<String> activeProfiles;
  private boolean overrideIfAlreadyExists;
  private QueryType queryType;
  private File repositoryPath;
  private String finishedByClass;
  private boolean offline;
  private boolean modifiedByCommand;

  public ProjectModel(
      POMDocument pomFile,
      List<POMDocument> parentPomFiles,
      Dependency dependency,
      boolean skipIfNewer,
      boolean useProperties,
      Set<String> activeProfiles,
      boolean overrideIfAlreadyExists,
      QueryType queryType,
      File repositoryPath,
      String finishedByClass,
      boolean offline) {
    this.pomFile = pomFile;
    this.parentPomFiles =
        CollectionUtils.isNotEmpty(parentPomFiles) ? parentPomFiles : Collections.emptyList();
    this.dependency = dependency;
    this.skipIfNewer = skipIfNewer;
    this.useProperties = useProperties;
    this.activeProfiles = activeProfiles;
    this.overrideIfAlreadyExists = overrideIfAlreadyExists;
    this.queryType = queryType != null ? queryType : QueryType.NONE;
    this.repositoryPath = repositoryPath;
    this.finishedByClass = finishedByClass;
    this.offline = offline;
    this.modifiedByCommand = false;
  }

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

  public Map<String, String> resolvedProperties() {
    Map<String, String> result = new LinkedHashMap<>();
    List<POMDocument> allPomFiles = allPomFiles(); // Implement this method

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

  public List<POMDocument> allPomFiles() {
    List<POMDocument> allFiles = new ArrayList<>();
    allFiles.add(pomFile);
    allFiles.addAll(parentPomFiles);
    return allFiles;
  }
}
