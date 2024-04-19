package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.RawFileCodemodTest;

/**
 * This test needs to be fixed once this bug is addressed:
 * https://github.com/pixee/codemodder-java/issues/359
 */
@Metadata(
    codemodType = MavenSecureURLCodemod.class,
    testResourceDir = "maven-non-https-url",
    //    expectingFixesAtLines = {22, 26, 30},
    dependencies = {})
final class MavenSecureURLCodemodTest implements RawFileCodemodTest {}
