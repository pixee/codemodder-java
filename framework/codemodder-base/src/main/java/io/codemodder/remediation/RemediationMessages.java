package io.codemodder.remediation;

/**
 * Holds common messages for remediation intended for human consumption as a part of CodeTF
 * reporting
 */
public interface RemediationMessages {

  /** No nodes at that location */
  String noNodesAtThatLocation = "No nodes at that location";

  /** Multiple nodes found at the given location and that may cause confusion */
  String multipleNodesFound =
      "Multiple nodes found at the given location and that may cause confusion";

  String ambiguousCodeShape = "Ambiguous code shape";
}
