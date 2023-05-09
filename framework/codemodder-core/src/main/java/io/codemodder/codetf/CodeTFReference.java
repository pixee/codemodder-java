package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    CodeTFReference that = (CodeTFReference) o;
    return Objects.equals(url, that.url) && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, description);
  }

  @Override
  public String toString() {
    return "CodeTFReference{" + "url='" + url + '\'' + ", description='" + description + '\'' + '}';
  }
}
