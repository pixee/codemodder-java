package io.codemodder.codemods;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;

@Metadata(
    codemodType = InsecureCookieCodemod.class,
    testResourceDir = "insecure-cookie",
    dependencies = {})
public class InsecureCookieCodemodTest implements CodemodTestMixin {}
