package io.codemodder.plugins.maven.operator;

import java.util.Objects;

/**
 * Represents a dependency in a Maven POM. A dependency consists of a group ID, artifact ID,
 * version, classifier, packaging, and scope.
 */
public class Dependency {
  private String groupId;
  private String artifactId;
  private String version;
  private String classifier;
  private String packaging;
  private String scope;

  /**
   * Initializes a new Dependency object with the provided attributes.
   *
   * @param groupId The group ID of the dependency.
   * @param artifactId The artifact ID of the dependency.
   * @param version The version of the dependency.
   * @param classifier The classifier of the dependency (may be null).
   * @param packaging The packaging type of the dependency (default is "jar" if null).
   * @param scope The scope of the dependency (default is "compile" if null).
   */
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

  @Override
  public String toString() {
    return String.join(":", groupId, artifactId, packaging, version);
  }

  /** Given a string, parses - and creates - a new dependency Object */
  static Dependency fromString(String str) {
    String[] elements = str.split(":");

    if (elements.length < 3) {
      throw new IllegalStateException("Give me at least 3 elements");
    }

    return new Dependency(elements[0], elements[1], elements[2], null, null, null);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Dependency that = (Dependency) o;
    return Objects.equals(groupId, that.groupId)
        && Objects.equals(artifactId, that.artifactId)
        && Objects.equals(version, that.version)
        && Objects.equals(classifier, that.classifier)
        && Objects.equals(packaging, that.packaging)
        && Objects.equals(scope, that.scope);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, classifier, packaging, scope);
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public void setArtifactId(String artifactId) {
    this.artifactId = artifactId;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getClassifier() {
    return classifier;
  }

  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  public String getPackaging() {
    return packaging;
  }

  public void setPackaging(String packaging) {
    this.packaging = packaging;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }
}
