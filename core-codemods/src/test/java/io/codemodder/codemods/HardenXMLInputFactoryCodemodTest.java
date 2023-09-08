package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HardenXMLInputFactoryCodemod.class,
    testResourceDir = "harden-xmlinputfactory",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class HardenXMLInputFactoryCodemodTest implements CodemodTestMixin {}
