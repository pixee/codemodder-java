package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Describes a "change" in a report. */
public final class CodeTFChange {

  private final int lineNumber;

  private final String description;

  private final String sourceControlUrl;

  private final Map<String, String> properties;

  private final List<CodeTFPackageAction> dependencies;

  @JsonCreator
  public CodeTFChange(
      @JsonProperty("lineNumber") final int lineNumber,
      @JsonProperty("properties") final Map<String, String> properties,
      @JsonProperty("description") final String description,
      @JsonProperty("dependencies") final List<CodeTFPackageAction> dependencies,
      @JsonProperty("sourceControlUrl") final String sourceControlUrl) {

    if (lineNumber < 1) {
      throw new IllegalArgumentException("line number must be positive");
    }

    this.lineNumber = lineNumber;
    this.properties = CodeTFValidator.toImmutableCopyOrEmptyOnNull(properties);
    this.dependencies = CodeTFValidator.toImmutableCopyOrEmptyOnNull(dependencies);
    this.sourceControlUrl = sourceControlUrl;
    this.description = description;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public String getDescription() {
    return description;
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public Optional<String> getSourceControlUrl() {
    return Optional.ofNullable(sourceControlUrl);
  }

  public List<CodeTFPackageAction> getDependencies() {
    return dependencies;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final CodeTFChange that = (CodeTFChange) o;
    return lineNumber == that.lineNumber
        && Objects.equals(description, that.description)
        && Objects.equals(sourceControlUrl, that.sourceControlUrl)
        && Objects.equals(properties, that.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(lineNumber, description, properties, sourceControlUrl);
  }

  @Override
  public String toString() {
    return "CodeTFChange{"
        + "lineNumber="
        + lineNumber
        + ", description='"
        + description
        + "', sourceControlUrl='"
        + sourceControlUrl
        + '\''
        + ", properties="
        + properties
        + '}';
  }
}
