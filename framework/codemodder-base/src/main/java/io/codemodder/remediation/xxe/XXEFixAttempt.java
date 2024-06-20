package io.codemodder.remediation.xxe;

/** Represents an attempt to fix an XXE vulnerability. */
record XXEFixAttempt(boolean isResponsibleFixer, boolean isFixed, String reasonNotFixed) {

  public XXEFixAttempt {
    if (!isResponsibleFixer && isFixed) {
      throw new IllegalStateException("Cannot be fixed by a non-responsible fixer");
    }
    if (!isFixed && reasonNotFixed == null) {
      throw new IllegalStateException("Reason must be provided if not fixed");
    }
  }
}
