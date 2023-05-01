package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public final class CodeTFReference {

  private final String url;
  private final String description;

  @JsonCreator
  public CodeTFReference(
      @JsonProperty("url") final String url,
      @JsonProperty("description") final String description) {
    this.url = CodeTFValidator.requireNonBlank(url);
    this.description = description;
  }

  public String getDescription() {
    return description;
  }

  public String getUrl() {
    return url;
  }
}
