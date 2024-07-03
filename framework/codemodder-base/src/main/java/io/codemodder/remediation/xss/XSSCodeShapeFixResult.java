package io.codemodder.remediation.xss;

/** The results of a fix attempt. */
record XSSCodeShapeFixResult(
    boolean isResponsibleFixer, boolean isFixed, String reasonNotFixed, int line) {
  XSSCodeShapeFixResult {
    if (!isResponsibleFixer && isFixed) {
      throw new IllegalArgumentException("Must be responsible fixer if fixed");
    }
    if (isFixed && reasonNotFixed != null) {
      throw new IllegalArgumentException("Cannot be fixed and have a reason not fixed");
    }
    if (isResponsibleFixer && !isFixed && reasonNotFixed == null) {
      throw new IllegalArgumentException("Must have a reason not fixed if not fixed");
    }
    if (isFixed && line < 0) {
      throw new IllegalArgumentException("Line must be positive");
    }
  }
}
