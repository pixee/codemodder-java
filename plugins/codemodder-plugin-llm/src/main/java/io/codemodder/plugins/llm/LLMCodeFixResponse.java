package io.codemodder.plugins.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** This is the model of the JSON response we expect to receive from the LLM with our */
public class LLMCodeFixResponse {

  @JsonProperty("changeRequired")
  private boolean changeRequired;

  @JsonProperty("analyses")
  private List<LLMLineAnalysis> analyses;

  @JsonProperty("fix")
  private String fix;

  public boolean isChangeRequired() {
    return changeRequired;
  }

  public List<LLMLineAnalysis> getAnalyses() {
    return analyses;
  }

  public String getFix() {
    return fix;
  }
}
