package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Describes an individual changeset entry. */
public final class CodeTFChangesetEntry {

  private final String path;

  private final String diff;

  private final List<CodeTFChange> changes;

  @JsonCreator
  public CodeTFChangesetEntry(
      @JsonProperty("path") final String path,
      @JsonProperty("diff") final String diff,
      @JsonProperty("changes") final List<CodeTFChange> changes) {
    this.path = CodeTFValidator.requireNonBlank(path);
    this.diff = CodeTFValidator.requireNonBlank(diff);
    this.changes = CodeTFValidator.toImmutableCopyOrEmptyOnNull(changes);
  }

  public String getPath() {
    return path;
  }

  public String getDiff() {
    return diff;
  }

  public List<CodeTFChange> getChanges() {
    return changes;
  }
}
