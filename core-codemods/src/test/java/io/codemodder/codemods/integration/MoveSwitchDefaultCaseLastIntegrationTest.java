package io.codemodder.codemods.integration;

import io.codemodder.codemods.integration.util.IntegrationTestMetadata;
import io.codemodder.codemods.integration.util.TestPropertiesMetadata;
import org.junit.jupiter.api.Test;

@IntegrationTestMetadata(
    codemodId = "move-switch-default-last",
    tests = {
            @TestPropertiesMetadata(testUrl = "http://localhost:%s?day=1",   httpVerb = "GET", expectedResponse = "Monday"),
            @TestPropertiesMetadata(testUrl = "http://localhost:%s?day=2",   httpVerb = "GET", expectedResponse = "Tuesday")
    })
public class MoveSwitchDefaultCaseLastIntegrationTest extends CodemodIntegrationTestSetup {
}
