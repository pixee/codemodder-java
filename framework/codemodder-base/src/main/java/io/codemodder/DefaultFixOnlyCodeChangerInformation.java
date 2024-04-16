package io.codemodder;

import io.codemodder.codetf.DetectorRule;

public class DefaultFixOnlyCodeChangerInformation implements FixOnlyCodeChangerInformation {

  private final VendorName vendorName;
  private final DetectorRule detectorRule;

  public DefaultFixOnlyCodeChangerInformation(
      final VendorName vendorName, final DetectorRule detectorRule) {
    this.vendorName = vendorName;
    this.detectorRule = detectorRule;
  }

  @Override
  public VendorName vendorName() {
    return vendorName;
  }

  @Override
  public DetectorRule getDetectorRule() {
    return detectorRule;
  }
}
