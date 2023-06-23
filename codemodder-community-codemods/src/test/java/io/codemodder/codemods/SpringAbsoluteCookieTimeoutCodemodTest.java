package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.RawFileCodemodTest;

@Metadata(
        codemodType = SpringAbsoluteCookieTimeoutCodemod.class,
        testResourceDir = "spring-absolute-cookie-timeout",
        dependencies = {})
final class SpringAbsoluteCookieTimeoutCodemodTest implements RawFileCodemodTest {

}
