package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.HardenXStreamCodemodTestProjectProvider;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenXStreamCodemod.class,
    testResourceDir = "harden-xstream",
    dependencies = "io.github.pixee:java-security-toolkit-xstream:1.0.2")
final class HardenXStreamCodemodTest implements CodemodTestMixin {}

@Metadata(
    codemodType = HardenXStreamCodemod.class,
    testResourceDir = "harden-xstream-denyTypes",
    dependencies = "io.github.pixee:java-security-toolkit-xstream:1.0.2",
    projectProviders = HardenXStreamCodemodTestProjectProvider.class)
final class HardenXStreamCodemodTest_denyTypes implements CodemodTestMixin {}
