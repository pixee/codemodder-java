package io.codemodder.plugins.maven.operator;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a Maven POM focusing on extracting dependency information without relying on any Maven
 * infrastructure.
 */
class QueryByParsing extends AbstractQueryCommand {

  private final Set<Dependency> dependencies = new LinkedHashSet<>();
  private final Set<Dependency> dependencyManagement =
      new TreeSet<>(
          new Comparator<Dependency>() {
            @Override
            public int compare(Dependency o1, Dependency o2) {
              if (o1 == o2) return 0;

              if (o1 == null) return 1;

              if (o2 == null) return -1;

              return new CompareToBuilder()
                  .append(o1.getGroupId(), o2.getGroupId())
                  .append(o1.getArtifactId(), o2.getArtifactId())
                  .toComparison();
            }
          });

  private final Map<String, String> properties = new LinkedHashMap<>();
  private final StrSubstitutor strSubstitutor = new StrSubstitutor(properties);

  @Override
  public void extractDependencyTree(Path outputPath, Path pomFilePath, ProjectModel c) {
    // Not implemented
  }

  /**
   * Executes the parsing strategy to extract dependency information from POM files.
   *
   * @param pm The ProjectModel containing input parameters for the operation.
   * @return `true` if the operation succeeds; otherwise, `false`.
   */
  @Override
  public boolean execute(ProjectModel pm) {
    /** Enlist all pom files given an hierarchy */
    List<POMDocument> pomFilesByHierarchy = pm.allPomFiles();
    Collections.reverse(pomFilesByHierarchy);

    for (POMDocument pomDocument : pomFilesByHierarchy) {
      updateProperties(pomDocument);
      updateDependencyManagement(pomDocument);
      updateDependencies(pomDocument);
    }

    this.result = dependencies;
    return true;
  }

  private void updateDependencyManagement(POMDocument pomDocument) {
    Collection<Dependency> dependencyManagementDependenciesToAdd = new ArrayList<>();

    Element dependencyManagementElement =
        pomDocument.getPomDocument().getRootElement().element("dependencyManagement");

    if (dependencyManagementElement != null) {
      List<Element> dependencyElements =
          dependencyManagementElement.element("dependencies").elements("dependency");

      for (Element dependencyElement : dependencyElements) {
        String groupId = getElementTextOrNull(dependencyElement, "groupId");
        String artifactId = getElementTextOrNull(dependencyElement, "artifactId");
        String version =
            Optional.ofNullable(dependencyElement.elementText("version")).orElse("UNKNOWN");
        String classifier = getElementTextOrNull(dependencyElement, "classifier");
        String packaging = getElementTextOrNull(dependencyElement, "packaging");

        try {
          version = strSubstitutor.replace(version);
        } catch (IllegalStateException e) {
          logger.warn("while interpolating version", e);
          version = "UNKNOWN";
        }

        Dependency dependency =
            new Dependency(groupId, artifactId, version, classifier, packaging, null);
        dependencyManagementDependenciesToAdd.add(dependency);
      }
    }

    this.dependencyManagement.addAll(dependencyManagementDependenciesToAdd);
  }

  private Dependency lookForDependencyManagement(String groupId, String artifactId) {
    for (Dependency dependency : dependencyManagement) {
      if (Objects.equals(dependency.getGroupId(), groupId)
          && Objects.equals(dependency.getArtifactId(), artifactId)) {
        return dependency;
      }
    }
    return null;
  }

  private void updateDependencies(POMDocument pomDocument) {
    Collection<Dependency> dependenciesToAdd = new ArrayList<>();

    Element dependenciesElement =
        pomDocument.getPomDocument().getRootElement().element("dependencies");

    if (dependenciesElement != null) {
      List<Element> dependencyElements = dependenciesElement.elements("dependency");

      for (Element dependencyElement : dependencyElements) {
        String groupId = getElementTextOrNull(dependencyElement, "groupId");
        String artifactId = getElementTextOrNull(dependencyElement, "artifactId");
        String version =
            Optional.ofNullable(dependencyElement.elementText("version")).orElse("UNKNOWN");

        Dependency proposedDependency = lookForDependencyManagement(groupId, artifactId);

        if (proposedDependency != null) {
          dependenciesToAdd.add(proposedDependency);
        } else {
          String classifier = getElementTextOrNull(dependencyElement, "classifier");
          String packaging = getElementTextOrNull(dependencyElement, "packaging");

          try {
            version = strSubstitutor.replace(version);
          } catch (IllegalStateException e) {
            logger.warn("while interpolating version", e);
            version = "UNKNOWN";
          }

          Dependency dependency =
              new Dependency(groupId, artifactId, version, classifier, packaging, null);
          dependenciesToAdd.add(dependency);
        }
      }
    }

    this.dependencies.addAll(dependenciesToAdd);
  }

  private String getElementTextOrNull(Element parent, String elementName) {
    Element child = parent.element(elementName);
    return child != null ? child.getText() : null;
  }

  /** Updates the Properties member variable based on whats on the POMDocument */
  private void updateProperties(POMDocument pomDocument) {
    Map<String, String> propsDefined = ProjectModel.propertiesDefinedOnPomDocument(pomDocument);

    for (Map.Entry<String, String> entry : propsDefined.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();

      if (!value.matches(RE_INTERPOLATION.pattern())) {
        properties.put(key, value);
      }

      if (!value.matches(RE_INTERPOLATION.pattern())) {
        String newValue;
        try {
          Matcher matcher = RE_INTERPOLATION.matcher(value);
          StringBuffer resultBuffer = new StringBuffer();
          while (matcher.find()) {
            String variable = matcher.group();
            String replacement = strSubstitutor.replace(variable);
            matcher.appendReplacement(resultBuffer, Matcher.quoteReplacement(replacement));
          }
          matcher.appendTail(resultBuffer);
          newValue = resultBuffer.toString();
        } catch (IllegalStateException e) {
          LOGGER.warn("while replacing variables: ", e);
          newValue = value;
        }

        properties.put(key, newValue);
      }
    }
  }

  private static final Pattern RE_INTERPOLATION =
      Pattern.compile(".*\\$\\{[\\p{Alnum}.\\-_]+\\}.*");
  private static final Logger logger = LoggerFactory.getLogger(QueryByParsing.class);
}
