package io.pixee.codefixer.java;

import java.util.Objects;

/** Models a Java dependency we might want to add. */
public interface DependencyGAV {

  String group();

  String artifact();

  String version();

  class Default implements DependencyGAV {

    private final String group;
    private final String artifact;
    private final String version;

    private Default(final String group, final String artifact, final String version) {
      this.group = Objects.requireNonNull(group, "group");
      this.artifact = Objects.requireNonNull(artifact, "artifact");
      this.version = Objects.requireNonNull(version, "version");
    }

    @Override
    public String group() {
      return group;
    }

    @Override
    public String artifact() {
      return artifact;
    }

    @Override
    public String version() {
      return version;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      final Default aDefault = (Default) o;
      return group.equals(aDefault.group)
          && artifact.equals(aDefault.artifact)
          && version.equals(aDefault.version);
    }

    @Override
    public int hashCode() {
      return Objects.hash(group, artifact, version);
    }

    @Override
    public String toString() {
      return "DependencyGAV{"
          + "group='"
          + group
          + '\''
          + ", artifact='"
          + artifact
          + '\''
          + ", version='"
          + version
          + '\''
          + '}';
    }
  }

  static DependencyGAV createDefault(
      final String group, final String artifact, final String version) {
    return new Default(group, artifact, version);
  }

  /**
   * The OpenPixee Java Security Toolkit is required by many weaves/visitors so we'll expose it
   * here.
   */
  DependencyGAV OPENPIXEE_JAVA_SECURITY_TOOLKIT =
      createDefault("io.github.pixee", "java-code-security-toolkit", "0.0.2");

  /** There are multiple XSS rules require an XSS encoder. */
  DependencyGAV OWASP_XSS_JAVA_ENCODER =
      DependencyGAV.createDefault("org.owasp.encoder", "encoder", "1.2.3");
}
