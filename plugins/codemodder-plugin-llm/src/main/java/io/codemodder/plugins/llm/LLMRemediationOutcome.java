package io.codemodder.plugins.llm;

/** Describes a possible remediation outcome. */
public interface LLMRemediationOutcome {

  /** A small, unique key that identifies this outcome. */
  String key();

  /** A description of the code that the LLM will attempt to use to match. */
  String description();

  /** Whether this outcome should lead to a code change. */
  boolean shouldApplyCodeChanges();
}
