package io.codemodder;

import io.codemodder.codetf.DetectorRule;

public record DefaultFixOnlyCodeChangerInformation(VendorName vendorName, DetectorRule detectorRule)
    implements FixOnlyCodeChangerInformation {}
