package io.pixee.codefixer.java;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;

/** Describes the root of the YAML configuration. */
public class Configuration {

  @JsonProperty("version")
  private String version;

  @JsonProperty("rules")
  private RuleConfiguration rules;

  @JsonProperty("paths")
  private PathConfiguration paths;

  public RuleConfiguration getRules() {
    return rules;
  }

  public PathConfiguration getPaths() {
    return paths;
  }

  public String getVersion() {
    return version;
  }

  /**
   * Loads the configuration from YAML directly.
   *
   * @param yaml the string containing the YAML
   */
  public static Configuration fromString(final String yaml) throws IOException {
    return new ObjectMapper(new YAMLFactory()).readValue(yaml, Configuration.class);
  }
}
