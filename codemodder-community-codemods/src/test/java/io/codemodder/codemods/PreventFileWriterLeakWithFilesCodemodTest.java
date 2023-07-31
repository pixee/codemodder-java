package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = PreventFileWriterLeakWithFilesCodemod.class,
    testResourceDir = "prevent-filewriter-leak-with-nio",
    dependencies = {})
final class PreventFileWriterLeakWithFilesCodemodTest implements CodemodTestMixin {}
