package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = ReplaceDefaultHttpClientCodemod.class,
    testResourceDir = "replace-apache-defaulthttpclient",
    dependencies = {})
final class ReplaceDefaultHttpClientCodemodTest implements CodemodTestMixin {}
