package io.codemodder;

import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;

public class DefaultFindingFactory implements FindingFactory {
  @Override
  public FixedFinding buildFixedFinding(String id, DetectorRule detectorRule) {
    return new FixedFinding(id, detectorRule);
  }
}
