package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = RemoveCollectionUtilsIsEmptyCodemod.class,
    testResourceDir = "remove-collectionutils-isempty",
    dependencies = {})
final class RemoveCollectionUtilsIsEmptyTest implements CodemodTestMixin {}
