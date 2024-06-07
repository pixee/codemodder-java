package triage;

import triage.update.FindingSeverity;

import java.util.Objects;

/**
 * Sonar has a different set of severity levels than we do. This enum maps Sonar severity levels to
 * our own. Sonar has also changed their severity ratings, so this contains mappings not only for
 * their stuff to our stuff, but from their old stuff (MAJOR, BLOCKER, etc) to our stuff. These
 * mappings are found here:
 *
 * <p><a href="https://docs.sonarsource.com/sonarqube/latest/user-guide/issues/">Sonar Issue
 * severity</a>
 */
public enum SonarSeverity {
  CRITICAL(FindingSeverity.HIGH),
  BLOCKER(FindingSeverity.HIGH),
  MAJOR(FindingSeverity.MEDIUM),
  MINOR(FindingSeverity.LOW),
  INFO(FindingSeverity.NOTE),
  HIGH(FindingSeverity.HIGH),
  MEDIUM(FindingSeverity.MEDIUM),
  LOW(FindingSeverity.LOW);

  private final FindingSeverity severity;

  private SonarSeverity(final FindingSeverity severity) {
    this.severity = Objects.requireNonNull(severity);
  }

  public FindingSeverity toSeverity() {
    return severity;
  }
}
