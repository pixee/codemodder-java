package io.codemodder.codemods.integration.tests;

import io.codemodder.codemods.integration.util.CodemodIntegrationTestMixin;
import io.codemodder.codemods.integration.util.IntegrationTestMetadata;
import io.codemodder.codemods.integration.util.IntegrationTestPropertiesMetadata;
import org.junit.jupiter.api.Disabled;

@Disabled
@IntegrationTestMetadata(
    codemodId = "move-switch-default-last",
    tests = {
      @IntegrationTestPropertiesMetadata(endpoint = "/test?day=1", expectedResponse = "Monday"),
      @IntegrationTestPropertiesMetadata(endpoint = "/test?day=2", expectedResponse = "Tuesday")
    })
public class MoveSwitchDefaultCaseLastCodemodIntegrationTest
    implements CodemodIntegrationTestMixin {}
