package io.codemodder.plugins.maven.operator;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class VersionByProperty extends AbstractVersionCommand {

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
