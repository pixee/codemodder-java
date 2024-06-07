package triage.update;

/** If a change needs to made to a finding. */
public enum FindingSeverity {
  CRITICAL(5),
  HIGH(4),
  MEDIUM(3),
  LOW(2),
  NOTE(1);

  private final int severityLevel;

  FindingSeverity(final int severityLevel) {
    this.severityLevel = severityLevel;
  }

  /** The level of severity, where highest is the most serious. */
  public int level() {
    return severityLevel;
  }
}
