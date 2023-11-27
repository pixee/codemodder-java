package io.codemodder.providers.sonar.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Describes where in the source code the issue resides. */
public class TextRange {

  @JsonProperty("startLine")
  private int startLine;

  @JsonProperty("endLine")
  private int endLine;

  @JsonProperty("startOffset")
  private int startOffset;

  @JsonProperty("endOffset")
  private int endOffset;

  public int getStartLine() {
    return startLine;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getEndOffset() {
    return endOffset;
  }

  public int getStartOffset() {
    return startOffset;
  }
}
