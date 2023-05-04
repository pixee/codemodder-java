package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Describes a SARIF "input" for the report. */
public final class CodeTFSarifInput {

  private final String artifact;

  private final String sha1;

  @JsonCreator
  public CodeTFSarifInput(
      @JsonProperty("artifact") final String artifact, @JsonProperty("sha1") final String sha1) {
    this.artifact = CodeTFValidator.requireNonBlank(artifact);
    this.sha1 = CodeTFValidator.requireNonBlank(sha1);
  }

  public String getArtifact() {
    return artifact;
  }

  public String getSha1() {
    return sha1;
  }
}
