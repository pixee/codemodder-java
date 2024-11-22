package io.codemodder.codemods.codeql;

import io.codemodder.DependencyGAV;
import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLHttpResponseSplittingCodemod.class,
    testResourceDir = "codeql-http-response-splitting",
    expectingFixesAtLines = {155},
    doRetransformTest = false,
    renameTestFile =
        "app/src/main/java/org/apache/roller/weblogger/webservices/oauth/AuthorizationServlet.java",
    dependencies = DependencyGAV.JAVA_SECURITY_TOOLKIT_GAV)
final class CodeQLHttpResponseSplittingCodemodTest implements CodemodTestMixin {}
