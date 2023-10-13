package io.codemodder.plugins.maven.operator;

import java.util.*;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 * Represents a strategy for querying version settings defined by the Maven Compiler Plugin
 * configuration in the POM.
 */
class VersionByCompilerDefinition extends AbstractVersionCommand {

  /**
   * Executes the strategy for querying version settings defined by the Maven Compiler Plugin
   * configuration in the POM.
   *
   * @param pm The ProjectModel containing the configuration and settings for the version query.
   * @return `true` if the query is successful, `false` otherwise.
   */
  @Override
  public boolean execute(ProjectModel pm) {
    Set<VersionDefinition> definedSettings =
        new TreeSet<>(AbstractVersionCommand.VERSION_KIND_COMPARATOR);

    List<String> parents =
        Arrays.asList(
            "//m:project/m:build/m:pluginManagement/m:plugins", "//m:project/m:build/m:plugins");

    Map<String, String> properties = pm.resolvedProperties();

    StrSubstitutor sub = new StrSubstitutor(properties);

    for (String parent : parents) {
      for (POMDocument doc : pm.allPomFiles()) {
        String pluginExpression =
            parent
                + "/m:plugin[./m:artifactId[text()='maven-compiler-plugin']]"
                + "//m:configuration";
        List<Node> compilerNodes = Util.selectXPathNodes(doc.getResultPom(), pluginExpression);

        if (!compilerNodes.isEmpty()) {
          for (Map.Entry<String, Kind> entry : AbstractVersionCommand.TYPE_TO_KIND.entrySet()) {
            String key = entry.getKey();
            Kind value = entry.getValue();

            for (Node compilerNode : compilerNodes) {
              Element childElement = ((Element) compilerNode).element(key);

              if (childElement != null) {
                String textTrim = childElement.getTextTrim();
                String substitutedText = sub.replace(textTrim);
                definedSettings.add(new VersionDefinition(value, substitutedText));
              }
            }
          }
        }
      }
    }

    result.addAll(definedSettings);

    return !definedSettings.isEmpty();
  }
}
