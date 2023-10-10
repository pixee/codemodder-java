package io.codemodder.plugins.maven.operator;

import kotlin.ranges.IntRange;
import kotlin.text.Regex;

public class MatchData {
  private final IntRange range;
  private final String content;
  private final String elementName;
  private final boolean hasAttributes;
  private final Regex modifiedContent;

  public MatchData(
      IntRange range,
      String content,
      String elementName,
      boolean hasAttributes,
      Regex modifiedContent) {
    assert range != null : "Range must not be null";
    assert content != null : "Content must not be null";
    assert elementName != null : "ElementName must not be null";

    this.range = range;
    this.content = content;
    this.elementName = elementName;
    this.hasAttributes = hasAttributes;
    this.modifiedContent = modifiedContent;
  }

  public boolean getHasAttributes() {
    return hasAttributes;
  }

  public IntRange getRange() {
    return range;
  }

  public String getContent() {
    return content;
  }

  public String getElementName() {
    return elementName;
  }

  public boolean isHasAttributes() {
    return hasAttributes;
  }

  public Regex getModifiedContent() {
    return modifiedContent;
  }
}
