package io.codemodder.plugins.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.Objects;

/**
 * A class that represents a threat analysis that is binary in its outcomes -- the analysis returns
 * "high" or "low".
 */
class BinaryThreatAnalysis {

  @JsonPropertyDescription("A detailed analysis of how the risk was assessed.")
  @JsonProperty(required = true)
  private String analysis;

  @JsonPropertyDescription("The risk of the security threat, either HIGH or LOW.")
  @JsonProperty(required = true)
  private BinaryThreatRisk risk;

  BinaryThreatAnalysis() {}

  BinaryThreatAnalysis(final String analysis, final BinaryThreatRisk risk) {
    this.analysis = analysis;
    this.risk = Objects.requireNonNull(risk);
  }

  /** Returns a detailed analysis of how the risk was assessed. */
  public String getAnalysis() {
    return analysis;
  }

  public BinaryThreatRisk getRisk() {
    return risk;
  }
}
