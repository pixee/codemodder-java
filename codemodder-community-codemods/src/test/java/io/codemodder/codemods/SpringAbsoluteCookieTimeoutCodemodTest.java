package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.RawFileCodemodTest;

@Metadata(
    codemodType = SpringAbsoluteCookieTimeoutCodemod.class,
    testResourceDir = "spring-absolute-cookie-timeout",
    renameTestFile = "src/main/resources/application.properties",
    dependencies = {})
final class SpringAbsoluteCookieTimeoutCodemodTest implements RawFileCodemodTest {}
