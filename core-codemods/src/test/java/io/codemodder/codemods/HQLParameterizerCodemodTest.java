package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = HQLParameterizationCodemod.class,
    testResourceDir = "hql-parameterizer",
    dependencies = {})
final class HQLParameterizerCodemodTest implements CodemodTestMixin {}
