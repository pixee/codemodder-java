package io.codemodder.codemods.integration;

import io.codemodder.codemods.integration.util.IntegrationTestMetadata;
import io.codemodder.codemods.integration.util.TestPropertiesMetadata;

@IntegrationTestMetadata(
    codemodId = "move-switch-default-last",
    tests = {
      @TestPropertiesMetadata(testUrl = "http://localhost:%s?day=1", expectedResponse = "Monday"),
      @TestPropertiesMetadata(testUrl = "http://localhost:%s?day=2", expectedResponse = "Tuesday")
    })
public class MoveSwitchDefaultCaseLastIntegrationTest extends CodemodIntegrationTestSetup {}
