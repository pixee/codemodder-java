package io.codemodder.codemods.integration;

import static io.codemodder.codemods.integration.util.TestApplicationRequestUtil.doRequest;
import static org.assertj.core.api.Assertions.assertThat;

import io.codemodder.codemods.integration.util.IntegrationTestMetadata;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.GenericContainer;

public class CodemodIntegrationTestSetup {

  private static final String endpointBasePath = "http://localhost:%s";
  private static GenericContainer<?> originalCodeContainer;
  private static GenericContainer<?> transformedCodeContainer;

  @TestFactory
  Stream<DynamicTest> generateTestCases() {
    IntegrationTestMetadata metadata = getClass().getAnnotation(IntegrationTestMetadata.class);
    if (metadata == null) {
      throw new IllegalArgumentException(
          "Test class must be annotated with @IntegrationTestMetadata");
    }

    final String codemodId = metadata.codemodId();

    originalCodeContainer = DockerContainerFactory.createContainer(codemodId, false);
    transformedCodeContainer = DockerContainerFactory.createContainer(codemodId, true);

    Stream.of(originalCodeContainer, transformedCodeContainer)
        .parallel()
        .forEach(GenericContainer::start);

    return Arrays.stream(metadata.tests())
        .map(
            test -> {
              final String originalContainerEndpointURL =
                  endpointBasePath.formatted(originalCodeContainer.getMappedPort(8080))
                      + test.endpoint();
              final String transformedContainerEndpointURL =
                  endpointBasePath.formatted(transformedCodeContainer.getMappedPort(8080))
                      + test.endpoint();
              final String httpVerb = test.httpVerb();
              final String expectedResponse = test.expectedResponse();

              return DynamicTest.dynamicTest(
                  "It_should_compare_application_behavior",
                  () -> {
                    final String originalCodeResponse =
                        doRequest(originalContainerEndpointURL, httpVerb);
                    final String transformedCodeResponse =
                        doRequest(transformedContainerEndpointURL, httpVerb);

                    assertThat(originalCodeResponse).isEqualTo(expectedResponse);
                    assertThat(transformedCodeResponse).isEqualTo(expectedResponse);
                  });
            });
  }

  @AfterAll
  public static void tearDown() {
    originalCodeContainer.stop();
    transformedCodeContainer.stop();
  }
}
