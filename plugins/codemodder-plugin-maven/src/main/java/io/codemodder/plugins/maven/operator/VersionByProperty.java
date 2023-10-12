package io.codemodder.plugins.maven.operator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Represents a strategy for querying version settings defined by properties in the Maven project.
 */
class VersionByProperty extends AbstractVersionCommand {

  /**
   * Executes the strategy for querying version settings defined by properties in the Maven project.
   *
   * @param pm The ProjectModel containing the configuration and settings for the version query.
   * @return `true` if the query is successful, `false` otherwise.
   */
  @Override
  public boolean execute(ProjectModel pm) {
    Set<VersionDefinition> definedProperties = new TreeSet<>(VERSION_KIND_COMPARATOR);

    for (Map.Entry<String, List<Pair<String, POMDocument>>> entry :
        pm.propertiesDefinedByFile().entrySet()) {
      String propertyName = entry.getKey();
      if (PROPERTY_TO_KIND.containsKey(propertyName)) {
        Kind kind = PROPERTY_TO_KIND.get(propertyName);

        if (kind != null) {
          definedProperties.add(new VersionDefinition(kind, entry.getValue().get(0).getFirst()));
        }
      }
    }

    result.addAll(definedProperties);

    return !definedProperties.isEmpty();
  }
}
