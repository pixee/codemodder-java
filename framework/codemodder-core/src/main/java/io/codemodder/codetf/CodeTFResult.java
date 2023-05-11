package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;

/** Describes the "result" section of a CodeTF document. */
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

  /** Create a new CodeTFResult builder based on an existing instance. */
  public static Builder basedOn(final CodeTFResult result) {
    return new Builder(result);
  }

  /** Builder for CodeTFResult which was based on an existing instance. */
  public static class Builder {
    private final CodeTFResult originalResult;
    private String updatedSummary;
    private String updatedDescription;
    private List<CodeTFReference> updatedReferences;

    private Builder(final CodeTFResult result) {
      this.originalResult = Objects.requireNonNull(result);
    }

    /** Update the CodeTFResult with the given summary. */
    public Builder withSummary(final String summary) {
      Objects.requireNonNull(summary);
      this.updatedSummary = summary;
      return this;
    }

    /** Update the CodeTFResult with the given description. */
    public Builder withDescription(final String description) {
      Objects.requireNonNull(description);
      this.updatedDescription = description;
      return this;
    }

    /** Update the CodeTFResult with additional references. */
    public Builder withAdditionalReferences(final List<CodeTFReference> references) {
      Objects.requireNonNull(references);
      if (updatedReferences == null) {
        updatedReferences = new ArrayList<>(originalResult.references);
      }
      updatedReferences.addAll(references);
      return this;
    }

    public CodeTFResult build() {
      return new CodeTFResult(
          originalResult.getCodemod(),
          updatedSummary != null ? updatedSummary : originalResult.getSummary(),
          updatedDescription != null ? updatedDescription : originalResult.getDescription(),
          originalResult.getFailedFiles(),
          updatedReferences != null ? updatedReferences : originalResult.getReferences(),
          originalResult.getProperties(),
          originalResult.getChangeset());
    }
  }
}
