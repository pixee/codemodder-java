package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/**
 * Top level CCF reporting object. This is the root object to be deserialized from a CCF input
 * stream.
 */
public final class CodeTFReport {

  private final CodeTFRun run;

  private final List<CodeTFResult> results;

  @JsonCreator
  public CodeTFReport(
      @JsonProperty("run") final CodeTFRun run,
      @JsonProperty("results") final List<CodeTFResult> results) {
    this.run = Objects.requireNonNull(run, "run");
    this.results = CodeTFValidator.toImmutableCopyOrEmptyOnNull(results);
  }

  public CodeTFRun getRun() {
    return run;
  }

  public List<CodeTFResult> getResults() {
    return results;
  }
}
