package io.codemodder;

import io.codemodder.codetf.DetectorRule;

public record DefaultFixOnlyCodeChanger(String vendorName, DetectorRule detectorRule)
    implements FixOnlyCodeChanger {}
