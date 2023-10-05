package io.codemodder;

import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A utility for helping meet the logging specification. */
final class Logs {

  private Logs() {}

  /** Describes the phase of execution we're in. These values are set by the spec. */
  enum ExecutionPhase {
    STARTING,
    SETUP,
    SCANNING,
    REPORT
  }

  /** Logs the start of a codemod phase. */
  static void logEnteringPhase(final ExecutionPhase phase) {
    Objects.requireNonNull(phase);
    log.debug("");
    log.debug("[{}]", phase.name().toLowerCase());
  }

  private static final Logger log = LoggerFactory.getLogger(Logs.class);
}
