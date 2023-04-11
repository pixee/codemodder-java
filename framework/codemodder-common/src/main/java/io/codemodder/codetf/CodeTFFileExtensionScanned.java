package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Describes a file extension scan report item. */
public final class CodeTFFileExtensionScanned {

  @JsonProperty("extension")
  private final String extension;

  @JsonProperty("count")
  private final int count;

  public CodeTFFileExtensionScanned(
      @JsonProperty("extension") final String extension, @JsonProperty("count") final int count) {
    this.extension = CodeTFValidator.requireNonBlank(extension);
    if (count < 0) {
      throw new IllegalArgumentException("count must be positive integer");
    }
    this.count = count;
  }

  public int getCount() {
    return count;
  }

  public String getExtension() {
    return extension;
  }
}
