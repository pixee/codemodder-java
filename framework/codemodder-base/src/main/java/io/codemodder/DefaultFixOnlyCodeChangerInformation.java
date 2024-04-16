package io.codemodder;

import io.codemodder.codetf.DetectorRule;

public record DefaultFixOnlyCodeChangerInformation(String vendorName, DetectorRule detectorRule)
    implements FixOnlyCodeChangerInformation {}
