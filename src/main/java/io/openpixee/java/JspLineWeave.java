package io.openpixee.java;

import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.Nullable;

/** Represents a weave done to a JSP line. */
public final class JspLineWeave {

  private final String supportingTaglib;
  private final String rebuiltLine;
  private final String ruleId;

  @Nullable private final DependencyGAV dependencyNeeded;

  public JspLineWeave(
      final String rebuiltLine,
      final String supportingTaglib,
      final String ruleId,
      final @Nullable DependencyGAV dependencyNeeded) {
    this.rebuiltLine = Objects.requireNonNull(rebuiltLine);
    this.ruleId = Objects.requireNonNull(ruleId);
    this.supportingTaglib = supportingTaglib;
    this.dependencyNeeded = dependencyNeeded;
  }

  public JspLineWeave(
      final String rebuiltLine, final String supportingTaglib, final String ruleId) {
    this(rebuiltLine, supportingTaglib, ruleId, null);
  }

  /** The replacement text for the line. */
  public String getRebuiltLine() {
    return rebuiltLine;
  }

  /** The rule ID associated with the weave. */
  public String getRuleId() {
    return ruleId;
  }

  /** The dependency needed for our new weave (if any.) */
  @Nullable
  public DependencyGAV getDependencyNeeded() {
    return dependencyNeeded;
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
