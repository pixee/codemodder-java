package io.codemodder.plugins.llm;

/** Describes a possible remediation outcome. */
public interface LLMRemediationOutcome {

  /** A small, unique key that identifies this outcome. */
  String key();

  /** A description of the code that the LLM will attempt to use to match. */
  String description();

  /** A description of the fix for cases that match this description. */
  String fix();

  /** Whether this outcome should lead to a code change. */
  boolean shouldApplyCodeChanges();
}
