package io.pixee.codefixer.java;

import java.util.Objects;

/** Represents a change made to the code. */
public final class Weave {

  /**
   * A human-readable-but-not-necessarily-descriptive string that describes the change, e.g.,
   * "harden-xml-factory".
   */
  private final String changeCode;

  /** Represents the original line number where this change was found to be applicable. */
  private final int lineNumber;

  private Weave(final int lineNumber, final String changeCode) {
    this.lineNumber = lineNumber;
    this.changeCode = Objects.requireNonNull(changeCode, "changeCode");
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final Weave weave = (Weave) o;
    return lineNumber == weave.lineNumber && changeCode.equals(weave.changeCode);
  }

  @Override
  public int hashCode() {
    return Objects.hash(changeCode, lineNumber);
  }

  public int lineNumber() {
    return lineNumber;
  }

  public String changeCode() {
    return changeCode;
  }

  public static Weave from(final int line, final String code) {
    return new Weave(line, code);
  }
}
