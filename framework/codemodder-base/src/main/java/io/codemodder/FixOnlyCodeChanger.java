package io.codemodder;

import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import java.util.Optional;

/**
 * A codemod that only fixes issues and does not perform its own detection, instead relying on
 * external analysis from other tools.
 *
 * <p>This is often provided via SARIF but can be provided by other means.
 */
public interface FixOnlyCodeChanger {

  /** Detection tool name. */
  String vendorName();

  /** A description of the rule. */
  DetectorRule getDetectorRule();

  /** Abstract method to build a {@link FixedFinding} object */
  Optional<FixedFinding> getFixedFinding(String id);
}
