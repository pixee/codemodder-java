package io.codemodder.llm;

import com.fasterxml.jackson.annotation.JsonProperty;

class LLMLineAnalysis {
  @JsonProperty("analysis")
  private String analysis;

  @JsonProperty("line")
  private int line;

  @JsonProperty("fixed")
  private boolean fixed;

  public boolean isFixed() {
    return fixed;
  }

  public String getAnalysis() {
    return analysis;
  }

  public int getLine() {
    return line;
  }
}
