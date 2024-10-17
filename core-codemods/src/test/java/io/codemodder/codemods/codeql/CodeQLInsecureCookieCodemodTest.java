package io.codemodder.codemods.codeql;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = CodeQLInsecureCookieCodemod.class,
    testResourceDir = "insecure-cookie",
    dependencies = {})
public class CodeQLInsecureCookieCodemodTest implements CodemodTestMixin {}
