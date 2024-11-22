package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
        codemodType = CodeQLXSSCodemod.class,
        testResourceDir = "codeql-xss",
        renameTestFile = "app/src/main/java/org/apache/roller/weblogger/ui/core/tags/calendar/CalendarTag.java",
        expectingFixesAtLines = 302,
        doRetransformTest = false,
        dependencies = {})
final class CodeQLXSSCodemodTest implements CodemodTestMixin { }
