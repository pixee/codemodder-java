package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = AddClarifyingBracesCodemod.class,
    testResourceDir = "add-clarifying-braces",
    dependencies = {})
final class AddClarifyingBracesCodemodTest implements CodemodTestMixin {}
