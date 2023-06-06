package io.codemodder.codemods;

import io.codemodder.testutils.Metadata;
import io.codemodder.testutils.RawFileCodemodTest;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

@Metadata(
    codemodType = SensitiveDataLoggingCodemod.class,
    testResourceDir = "sensitive-data-logging",
    dependencies = {})
@EnabledIfEnvironmentVariable(named = "CODEMODDER_OPENAI_API_KEY", matches = ".+")
final class SensitiveDataLoggingCodemodTest implements RawFileCodemodTest {}
