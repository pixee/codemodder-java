package io.pixee.codefixer.java;

import java.util.Objects;
import java.util.Optional;

/** Represents a weave done to a JSP line. */
public final class JspLineWeave {

  private final String supportingTaglib;
  private final String rebuiltLine;
  private final String ruleId;

  public JspLineWeave(
      final String rebuiltLine, final String supportingTaglib, final String ruleId) {
    this.rebuiltLine = Objects.requireNonNull(rebuiltLine);
    this.ruleId = Objects.requireNonNull(ruleId);
    this.supportingTaglib = supportingTaglib;
  }

  /** The replacement text for the line. */
  public String getRebuiltLine() {
    return rebuiltLine;
  }

  /** The rule ID assocated with the weave. */
  public String getRuleId() {
    return ruleId;
  }

  /**
   * The taglib definition needed to support this newly rebuilt line, which is only needed
   * sometimes.
   */
  public Optional<String> getSupportingTaglib() {
    if (supportingTaglib == null) {
      return Optional.empty();
    }
    return Optional.of(supportingTaglib);
  }
}
