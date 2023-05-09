package io.codemodder;

/** Represents a codemod author's confidence that changes wll be safe and effective. */
public enum ReviewGuidance {
  MERGE_AFTER_REVIEW,
  MERGE_AFTER_CURSORY_REVIEW,
  MERGE_WITHOUT_REVIEW
}
