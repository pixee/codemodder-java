package io.pixee.codefixer.java;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Describes the 'paths' section of the YAML configuration.
 */
public class PathConfiguration {

  @JsonProperty("includes")
  private List<String> includes;

  @JsonProperty("excludes")
  private List<String> excludes;

  public List<String> getIncludes() {
    return includes;
  }

  public List<String> getExcludes() {
    return excludes;
  }
}
