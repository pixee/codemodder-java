package io.codemodder;

import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;

public interface FindingFactory {

  FixedFinding buildFixedFinding(String id, DetectorRule detectorRule);
}
