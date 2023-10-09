package io.codemodder.plugins.maven.operator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@EqualsAndHashCode
@Getter
@Setter
public class Dependency {
  private String groupId;
  private String artifactId;
  private String version;
  private String classifier;
  private String packaging;
  private String scope;

  public Dependency(
      String groupId,
      String artifactId,
      String version,
      String classifier,
      String packaging,
      String scope) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classifier = classifier;
    this.packaging = packaging != null ? packaging : "jar";
    this.scope = scope != null ? scope : "compile";
  }

  public Dependency(String groupId, String artifactId, String version) {
    this(groupId, artifactId, version, null, null, null);
  }

  public Dependency(String groupId, String artifactId) {
    this(groupId, artifactId, null, null, null, null);
  }

  @Override
  public String toString() {
    return String.join(":", groupId, artifactId, packaging, version);
  }

  public static Dependency fromString(String str) {
    String[] elements = str.split(":");

    if (elements.length < 3) {
      throw new IllegalStateException("Give me at least 3 elements");
    }

    return new Dependency(elements[0], elements[1], elements[2], null, null, null);
  }
}
