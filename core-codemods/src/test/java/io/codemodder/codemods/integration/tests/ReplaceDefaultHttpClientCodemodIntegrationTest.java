package io.codemodder.codemods.integration.tests;

import io.codemodder.codemods.integration.util.CodemodIntegrationTestMixin;
import io.codemodder.codemods.integration.util.IntegrationTestMetadata;
import io.codemodder.codemods.integration.util.IntegrationTestPropertiesMetadata;

/**
 * Test project: the project used for this test is using DefaultHttpClient to do a request to
 * https://restcountries.com/, the expectation is that DefaultHttpClient will be transformed into
 * HttpClientBuilder and the request is still working as expected. Test project location:
 * resources/test-applications/replace-apache-defaulthttpclient
 */
@IntegrationTestMetadata(
    codemodId = "replace-apache-defaulthttpclient",
    tests = {
      @IntegrationTestPropertiesMetadata(
          endpoint = "/test/country/mexico/capital",
          expectedResponse = "Mexico City")
    })
public class ReplaceDefaultHttpClientCodemodIntegrationTest
    implements CodemodIntegrationTestMixin {}
