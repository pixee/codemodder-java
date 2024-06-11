package io.codemodder.sonar.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents a Sonar "issue". */
public final class Issue extends SonarFinding {

  @JsonProperty("rule")
  private String rule;

  public String getRule() {
    return rule;
  }

  @Override
  public String typeName() {
    return "issues";
  }

  @Override
  public String rule() {
    return rule;
  }
}
