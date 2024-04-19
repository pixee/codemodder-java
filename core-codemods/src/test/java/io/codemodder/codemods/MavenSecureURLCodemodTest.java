package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.RawFileCodemodTest;

@Metadata(
    codemodType = MavenSecureURLCodemod.class,
    testResourceDir = "maven-non-https-url",
    expectingFixesAtLines = {22},
    dependencies = {})
final class MavenSecureURLCodemodTest implements RawFileCodemodTest {}
