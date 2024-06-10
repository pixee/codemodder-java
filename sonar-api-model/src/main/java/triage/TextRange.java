package triage;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Represents a range of text in a file. */
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

  public int getStartOffset() {
    return startOffset;
  }

  public int getEndOffset() {
    return endOffset;
  }
}
