package io.codemodder.plugins.maven.operator;

/**
 * Represents a tuple (kind / version string) applicable from a pom.xml file
 *
 * <p>For Internal Consumption (thus Internal)
 */
class VersionDefinition {
  private Kind kind;
  private String value;

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
