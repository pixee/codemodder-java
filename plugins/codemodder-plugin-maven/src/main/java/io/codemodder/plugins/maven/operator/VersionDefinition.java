package io.codemodder.plugins.maven.operator;

/**
 * Represents a tuple containing version information (kind / version string) applicable to a pom.xml
 * file.
 *
 * <p>This class is intended for internal consumption and should not be used directly by external
 * code.
 */
class VersionDefinition {
  private Kind kind;
  private String value;

  /**
   * Constructs a new VersionDefinition with the specified kind and version string.
   *
   * @param kind The kind of version information.
   * @param value The version string.
   */
  public VersionDefinition(Kind kind, String value) {
    this.kind = kind;
    this.value = value;
  }

  public void setKind(Kind kind) {
    this.kind = kind;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public Kind getKind() {
    return kind;
  }

  public String getValue() {
    return value;
  }
}
