package io.codemodder;

import io.codemodder.codetf.DetectorRule;

public interface FixOnlyCodeChangerInformation {
  String vendorName();

  DetectorRule detectorRule();
}
