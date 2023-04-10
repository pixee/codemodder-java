package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Describes an "input" for the CCF report. */
public final class CodeTFInput {

  private final String artifact;

  private final String sha1;

  private final String vendor;

  @JsonCreator
  public CodeTFInput(
      @JsonProperty("artifact") final String artifact,
      @JsonProperty("sha1") final String sha1,
      @JsonProperty("vendor") final String vendor) {
    this.artifact = CodeTFValidator.requireNonBlank(artifact);
    this.sha1 = CodeTFValidator.requireNonBlank(sha1);
    this.vendor = CodeTFValidator.requireNonBlank(vendor);
  }

  public String getArtifact() {
    return artifact;
  }

  public String getSha1() {
    return sha1;
  }

  public String getVendor() {
    return vendor;
  }
}
