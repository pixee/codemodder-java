package io.codemodder.codemods.integration;

import io.codemodder.codemods.integration.util.IntegrationTestMetadata;
import io.codemodder.codemods.integration.util.TestPropertiesMetadata;

@IntegrationTestMetadata(
    codemodId = "verbose-request-mapping",
    tests = {
      @TestPropertiesMetadata(endpoint = "/test/hello", expectedResponse = "Hello World!"),
      @TestPropertiesMetadata(endpoint = "/test/welcome", expectedResponse = "Welcome"),
      @TestPropertiesMetadata(endpoint = "/test/greet", expectedResponse = "Greetings!"),
      @TestPropertiesMetadata(
          endpoint = "/test/update",
          httpVerb = "PUT",
          expectedResponse = "Data Updated!"),
      @TestPropertiesMetadata(
          endpoint = "/test/delete",
          httpVerb = "DELETE",
          expectedResponse = "Data Deleted!"),
      @TestPropertiesMetadata(
          endpoint = "/test/create",
          httpVerb = "POST",
          expectedResponse = "Data Created!")
    })
public class VerboseRequestMappingCodemodIntegrationTest extends CodemodIntegrationTestMixin {}
