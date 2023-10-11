package io.codemodder.plugins.maven.operator;

import java.util.*;
import org.apache.commons.lang3.builder.CompareToBuilder;

class AbstractVersionCommand extends AbstractCommand {

  public Set<VersionDefinition> result = new TreeSet<>(VERSION_KIND_COMPARATOR);

  public static final Comparator<VersionDefinition> VERSION_KIND_COMPARATOR =
      new Comparator<VersionDefinition>() {
        @Override
        public int compare(VersionDefinition o1, VersionDefinition o2) {
          if (o1 == null) return 1;
          if (o2 == null) return -1;

          return new CompareToBuilder().append(o1.getKind(), o2.getKind()).toComparison();
        }
      };

  public static final Map<String, Kind> TYPE_TO_KIND = new HashMap<>();
  public static final Map<String, Kind> PROPERTY_TO_KIND = new HashMap<>();

  static {
    TYPE_TO_KIND.put("source", Kind.SOURCE);
    TYPE_TO_KIND.put("target", Kind.TARGET);
    TYPE_TO_KIND.put("release", Kind.RELEASE);

    for (Map.Entry<String, Kind> entry : TYPE_TO_KIND.entrySet()) {
      PROPERTY_TO_KIND.put("maven.compiler." + entry.getKey(), entry.getValue());
    }
  }
}
