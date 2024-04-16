package io.codemodder;

import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;

public interface FixOnlyCodeChangerInformation {
  VendorName vendorName();

  DetectorRule detectorRule();

  default FixedFinding buildFixedFinding(String id) {
    return new FixedFinding(id, detectorRule());
  }
}
