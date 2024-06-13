package io.codemodder.sonar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.codemodder.sonar.model.update.FindingSeverity;

/** Represents a Sonar "hotspot". */
public final class Hotspot extends SonarFinding {

  @JsonProperty("securityCategory")
  private String securityCategory;

  @JsonProperty("ruleKey")
  private String ruleKey;

  @JsonProperty("vulnerabilityProbability")
  private FindingSeverity vulnerabilityProbability;

  public FindingSeverity getVulnerabilityProbability() {
    return vulnerabilityProbability;
  }

  public String getSecurityCategory() {
    return securityCategory;
  }

  public String getRuleKey() {
    return ruleKey;
  }

  @Override
  public String typeName() {
    return "hotspots";
  }

  @Override
  public String rule() {
    return ruleKey;
  }
}
