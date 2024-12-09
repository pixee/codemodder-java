package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLPotentiallyUnsafeCryptoAlgorithmCodemod.class,
    testResourceDir = "codeql-potentially-unsafe-crypto-algorithm",
    renameTestFile = "app/src/main/java/org/apache/roller/weblogger/util/WSSEUtilities.java",
    expectingFixesAtLines = {38},
    doRetransformTest = false,
    dependencies = {})
final class CodeQLPotentiallyUnsafeCryptoAlgorithmCodemodTest implements CodemodTestMixin {}
