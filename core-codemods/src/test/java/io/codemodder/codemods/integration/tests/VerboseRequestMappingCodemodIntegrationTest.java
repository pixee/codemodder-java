package io.codemodder.codemods.integration.tests;

import io.codemodder.codemods.integration.util.CodemodIntegrationTestMixin;
import io.codemodder.codemods.integration.util.IntegrationTestMetadata;
import io.codemodder.codemods.integration.util.IntegrationTestPropertiesMetadata;

@IntegrationTestMetadata(
    codemodId = "verbose-request-mapping",
    tests = {
      @IntegrationTestPropertiesMetadata(
          endpoint = "/test/hello",
          expectedResponse = "Hello World!"),
      @IntegrationTestPropertiesMetadata(endpoint = "/test/welcome", expectedResponse = "Welcome"),
      @IntegrationTestPropertiesMetadata(endpoint = "/test/greet", expectedResponse = "Greetings!"),
      @IntegrationTestPropertiesMetadata(
          endpoint = "/test/update",
          httpVerb = "PUT",
          expectedResponse = "Data Updated!"),
      @IntegrationTestPropertiesMetadata(
          endpoint = "/test/delete",
          httpVerb = "DELETE",
          expectedResponse = "Data Deleted!"),
      @IntegrationTestPropertiesMetadata(
          endpoint = "/test/create",
          httpVerb = "POST",
          expectedResponse = "Data Created!")
    })
public class VerboseRequestMappingCodemodIntegrationTest implements CodemodIntegrationTestMixin {}
