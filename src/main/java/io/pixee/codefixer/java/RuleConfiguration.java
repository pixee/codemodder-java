package io.pixee.codefixer.java;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;

/** Describes the 'rules' section of the YAML configuration. */
public class RuleConfiguration {

  @JsonProperty("default")
  private String defaultRuleSetting;

  @JsonProperty("exceptions")
  private List<String> exceptions;

  public List<String> getExceptions() {
    return exceptions != null ? exceptions : Collections.emptyList();
  }

  public String getDefaultRuleSetting() {
    return defaultRuleSetting != null ? defaultRuleSetting : DEFAULT_RULE_SETTING;
  }

  private static final String DEFAULT_RULE_SETTING = "enabled";
}
