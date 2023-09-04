package io.codemodder.plugins.llm;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

final class BinaryThreatAnalysisAndFix extends BinaryThreatAnalysis {

  @JsonPropertyDescription(
      "The fix as a diff patch in unified format. Required if the risk is HIGH.")
  private String fix;

  @JsonPropertyDescription("A short description of the fix. Required if the file is fixed.")
  private String fixDescription;

  public String getFix() {
    return fix;
  }

  public String getFixDescription() {
    return fixDescription;
  }
}
