package io.pixee.codefixer.java;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Describes the 'rules' section of the YAML configuration. */
public class RuleConfiguration {

  @JsonProperty("default")
  private String defaultRuleSetting;

  @JsonProperty("exceptions")
  private List<String> exceptions;

  public List<String> getExceptions() {
    return exceptions;
  }

  public String getDefaultRuleSetting() {
    return defaultRuleSetting;
  }
}
