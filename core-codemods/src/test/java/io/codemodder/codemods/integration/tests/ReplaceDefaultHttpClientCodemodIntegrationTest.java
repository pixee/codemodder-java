package io.codemodder.codemods.integration.tests;

import io.codemodder.codemods.integration.util.CodemodIntegrationTestMixin;
import io.codemodder.codemods.integration.util.IntegrationTestMetadata;
import io.codemodder.codemods.integration.util.IntegrationTestPropertiesMetadata;

@IntegrationTestMetadata(
        codemodId = "replace-apache-defaulthttpclient",
        tests = {
                @IntegrationTestPropertiesMetadata(endpoint = "/test/country/mexico/capital", expectedResponse = "Mexico City")
        })
public class ReplaceDefaultHttpClientCodemodIntegrationTest implements CodemodIntegrationTestMixin {
}
