package io.codemodder;

import io.codemodder.codetf.DetectorRule;

public interface FixOnlyCodeChanger {
  String vendorName();

  DetectorRule detectorRule();
}
