package io.codemodder;

import java.util.Objects;
import java.util.Optional;

/** Models a Java dependency we might want to add. */
public interface DependencyGAV {

  /** The group of the dependency. For example, {@code org.owasp} or {@code com.google.guava}. */
  String group();

  /** The artifact of the dependency. For example, {@code owasp-java-html-sanitizer}. */
  String artifact();

  /** The version of the dependency. For example, {@code 2019.2}. */
  String version();

  /**
   * The justification for adding this dependency. For example, {@code "We need this to sanitize
   * HTML"}
   */
  Optional<String> justification();

  /** The repository URL for this dependency's source control. If unknown, this will be empty. */
  Optional<String> repositoryUrl();

  /** Whether this dependency has transitive dependencies. If unknown, this will be empty. */
  Optional<Boolean> hasNoTransitiveDependencies();

  /** The license for this dependency. */
  Optional<String> license();

  class Default implements DependencyGAV {

    private final String group;
    private final String artifact;
    private final String version;
    private final String license;
    private final String justification;
    private final String repositoryUrl;
    private final Boolean noTransitiveDependencies;

    private Default(
        final String group,
        final String artifact,
        final String version,
        final String justification,
        final String license,
        final String repositoryUrl,
        final Boolean noTransitiveDependencies) {
      this.group = Objects.requireNonNull(group, "group");
      this.artifact = Objects.requireNonNull(artifact, "artifact");
      this.version = Objects.requireNonNull(version, "version");
      this.justification = justification;
      this.license = license;
      this.repositoryUrl = repositoryUrl;
      this.noTransitiveDependencies = noTransitiveDependencies;
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
    public Optional<String> justification() {
      return Optional.ofNullable(justification);
    }

    @Override
    public Optional<Boolean> hasNoTransitiveDependencies() {
      return Optional.ofNullable(noTransitiveDependencies);
    }

    @Override
    public Optional<String> repositoryUrl() {
      return Optional.ofNullable(repositoryUrl);
    }

    @Override
    public Optional<String> license() {
      return Optional.ofNullable(license);
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

  /**
   * Create a new {@link DependencyGAV} with the given group, artifact, and version -- the bare
   * minimum to describe a dependency for injection. If more information is available, consider
   * using {@link #createDefault(String, String, String, String, String, String, Boolean)} which
   * allows for more actionable information for developers to make informed choices about
   * dependencies.
   */
  static DependencyGAV createDefault(
      final String group, final String artifact, final String version) {
    return new Default(group, artifact, version, null, null, null, null);
  }

  /**
   * Create a new {@link DependencyGAV} with the given group, artifact, version, justification,
   * license, and repository URL.
   *
   * @param group the group of the dependency
   * @param artifact the artifact of the dependency
   * @param version the version of the dependency
   * @param justification a short text for the justification for adding this dependency (can be
   *     null)
   * @param license the license for this dependency (see {@link DependencyLicenses} for common
   *     licenses) (can be null)
   * @param repositoryUrl the repository URL for this dependency's source control (can be null)
   */
  static DependencyGAV createDefault(
      final String group,
      final String artifact,
      final String version,
      final String justification,
      final String license,
      final String repositoryUrl,
      final Boolean noTransitiveDependencies) {
    return new Default(
        group, artifact, version, justification, license, repositoryUrl, noTransitiveDependencies);
  }

  String JAVA_SECURITY_TOOLKIT_VERSION = "1.1.3";
  String JAVA_SECURITY_TOOLKIT_GAV =
      "io.github.pixee:java-security-toolkit:" + JAVA_SECURITY_TOOLKIT_VERSION;

  /**
   * The pixee Java Security Toolkit is required by many weaves/visitors, so we'll expose it here.
   */
  DependencyGAV JAVA_SECURITY_TOOLKIT =
      createDefault(
          "io.github.pixee",
          "java-security-toolkit",
          JAVA_SECURITY_TOOLKIT_VERSION,
          "This library holds security tools for protecting Java API calls.",
          DependencyLicenses.MIT,
          "https://github.com/pixee/java-security-toolkit",
          false);

  /** There are multiple XSS rules require an XSS encoder. */
  DependencyGAV OWASP_XSS_JAVA_ENCODER =
      DependencyGAV.createDefault(
          "org.owasp.encoder",
          "encoder",
          "1.2.3",
          "This library holds XSS encoders for different contexts.",
          DependencyLicenses.BSD_3_CLAUSE,
          "https://github.com/OWASP/owasp-java-encoder",
          true);
}
