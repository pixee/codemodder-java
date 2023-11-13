package io.codemodder.plugins.maven.operator;

import io.codemodder.DependencyGAV;
import java.util.Objects;

/**
 * Represents a dependency in a Maven POM. A dependency consists of a group ID, artifact ID,
 * version, classifier, packaging, and scope.
 */
class Dependency {
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
  Dependency(
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

  Dependency(final DependencyGAV newDependencyGAV) {
    this(
        newDependencyGAV.group(),
        newDependencyGAV.artifact(),
        newDependencyGAV.version(),
        null,
        null,
        null);
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

  /**
   * Computes the hash code of this object without considering the version.
   *
   * <p>This method calculates the hash code based on the values of the group ID, artifact ID,
   * classifier, packaging, and scope fields, while ignoring the version field.
   *
   * @return The computed hash code without the version.
   */
  public int hashWithoutVersion() {
    return Objects.hash(groupId, artifactId, classifier, packaging, scope);
  }

  /**
   * Checks if this Dependency object matches another Dependency without considering the version.
   *
   * <p>This method compares the hash code of this Dependency with the hash code of another
   * Dependency, excluding the version information. If the hash codes are equal, the two
   * dependencies are considered a match without taking version into account.
   *
   * @param dependency The Dependency object to compare with this one.
   * @return true if the dependencies match without considering the version, false otherwise.
   */
  public boolean matchesWithoutConsideringVersion(final Dependency dependency) {
    return hashWithoutVersion() == dependency.hashWithoutVersion();
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
