package io.codemodder;

import io.codemodder.codetf.DetectionTool;

/**
 * A codemod that only fixes issues and does not perform its own detection, instead relying on
 * external analysis from other tools.
 *
 * <p>This is often provided via SARIF but can be provided by other means.
 */
public interface FixOnlyCodeChanger {

  /** Detection tool metadata for this codemod. */
  DetectionTool getDetectionTool();
}
