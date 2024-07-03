package io.codemodder.remediation;

/**
 * Holds common messages for remediation intended for human consumption as a part of CodeTF
 * reporting
 */
public interface RemediationMessages {

  /** No calls at that location */
  String noCallsAtThatLocation = "No calls at that location";

  /** Multiple calls found at the given location and that may cause confusion */
  String multipleCallsFound =
      "Multiple calls found at the given location and that may cause confusion";

  String ambiguousCodeShape = "Ambiguous code shape";
}
