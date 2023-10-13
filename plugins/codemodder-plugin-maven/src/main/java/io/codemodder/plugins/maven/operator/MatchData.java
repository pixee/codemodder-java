package io.codemodder.plugins.maven.operator;

import kotlin.ranges.IntRange;
import kotlin.text.Regex;

/** Data Class used to keep track of matches (ranges, content, referring tag name) */
class MatchData {
  private final IntRange range;
  private final String content;
  private final String elementName;
  private final boolean hasAttributes;
  private final Regex modifiedContent;

  /**
   * Data Class used to keep track of matches (ranges, content, referring tag name).
   *
   * @param range The range (start and end positions) of the matched content.
   * @param content The content of the matched element.
   * @param elementName The name of the referring tag.
   * @param hasAttributes A boolean indicating whether the element has attributes.
   * @param modifiedContent A regular expression representing the modified content of the element.
   */
  MatchData(
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

  boolean getHasAttributes() {
    return hasAttributes;
  }

  IntRange getRange() {
    return range;
  }

  String getContent() {
    return content;
  }

  String getElementName() {
    return elementName;
  }

  boolean isHasAttributes() {
    return hasAttributes;
  }

  Regex getModifiedContent() {
    return modifiedContent;
  }
}
