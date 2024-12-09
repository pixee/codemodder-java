package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLLogInjectionCodemod.class,
    testResourceDir = "codeql-log-injection",
    renameTestFile =
        "app/src/main/java/org/apache/roller/weblogger/ui/struts2/editor/Templates.java",
    doRetransformTest = false,
    expectingFixesAtLines = {124},
    dependencies = {})
final class CodeQLLogInjectionCodemodTest implements CodemodTestMixin {}
