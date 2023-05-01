package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CodeTFResult {

  private final String codemodId;
  private final String summary;
  private final String description;
  private final Set<String> failedFiles;
  private final List<CodeTFReference> references;
  private final Map<String, String> properties;
  private final List<CodeTFChangesetEntry> changeset;

  @JsonCreator
  public CodeTFResult(
      @JsonProperty("codemod") final String codemodId,
      @JsonProperty("summary") final String summary,
      @JsonProperty("description") final String description,
      @JsonProperty("failedFiles") final Set<String> failedFiles,
      @JsonProperty("references") final List<CodeTFReference> references,
      @JsonProperty("properties") final Map<String, String> properties,
      @JsonProperty("changeset") final List<CodeTFChangesetEntry> changeset) {
    this.codemodId = CodeTFValidator.requireNonBlank(codemodId);
    this.summary = CodeTFValidator.requireNonBlank(summary);
    this.description = CodeTFValidator.requireNonBlank(description);
    this.failedFiles = CodeTFValidator.toImmutableCopyOrEmptyOnNull(failedFiles);
    this.references = CodeTFValidator.toImmutableCopyOrEmptyOnNull(references);
    this.properties = CodeTFValidator.toImmutableCopyOrEmptyOnNull(properties);
    this.changeset = Objects.requireNonNull(changeset);
  }

  public String getCodemodId() {
    return codemodId;
  }

  public String getSummary() {
    return summary;
  }

  public String getDescription() {
    return description;
  }

  public Set<String> getFailedFiles() {
    return failedFiles;
  }

  public List<CodeTFReference> getReferences() {
    return references;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public List<CodeTFChangesetEntry> getChangeset() {
    return changeset;
  }
}
