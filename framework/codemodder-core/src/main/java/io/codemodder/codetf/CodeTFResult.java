package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CodeTFResult {

  private final String codemod;
  private final String summary;
  private final String description;
  private final Set<String> failedFiles;
  private final List<CodeTFReference> references;
  private final Map<String, String> properties;
  private final List<CodeTFChangesetEntry> changeset;

  @JsonCreator
  public CodeTFResult(
      @JsonProperty(value = "codemod", index = 1) final String codemod,
      @JsonProperty(value = "summary", index = 2) final String summary,
      @JsonProperty(value = "description", index = 3) final String description,
      @JsonProperty(value = "failedFiles", index = 4) final Set<String> failedFiles,
      @JsonProperty(value = "references", index = 5) final List<CodeTFReference> references,
      @JsonProperty(value = "properties", index = 6) final Map<String, String> properties,
      @JsonProperty(value = "changeset", index = 7) final List<CodeTFChangesetEntry> changeset) {
    this.codemod = CodeTFValidator.requireNonBlank(codemod);
    this.summary = CodeTFValidator.requireNonBlank(summary);
    this.description = CodeTFValidator.requireNonBlank(description);
    this.failedFiles = CodeTFValidator.toImmutableCopyOrEmptyOnNull(failedFiles);
    this.references = CodeTFValidator.toImmutableCopyOrEmptyOnNull(references);
    this.properties = CodeTFValidator.toImmutableCopyOrEmptyOnNull(properties);
    this.changeset = Objects.requireNonNull(changeset);
  }

  public String getCodemod() {
    return codemod;
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
