package io.codemodder.codemods.integration.util;

import static io.codemodder.codemods.integration.util.TestApplicationRequests.doRequest;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.testcontainers.containers.GenericContainer;

/** The codemods integration tests mixin */
public interface CodemodIntegrationTestMixin {

  String endpointBasePath = "http://localhost:%s";

  /**
   * Setup and start test containers.
   *
   * @return a stream of dynamic tests generated based on the data extracted from {@link
   *     IntegrationTestMetadata}
   */
  @TestFactory
  default Stream<DynamicTest> generateTestCases() {
    IntegrationTestMetadata metadata = getClass().getAnnotation(IntegrationTestMetadata.class);
    if (metadata == null) {
      throw new IllegalArgumentException(
          "Test class must be annotated with @IntegrationTestMetadata");
    }

    final String codemodId = metadata.codemodId();

    GenericContainer<?> originalCodeContainer =
        DockerContainerFactory.createContainer(codemodId, false);
    GenericContainer<?> transformedCodeContainer =
        DockerContainerFactory.createContainer(codemodId, true);

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
            })
        .onClose(
            () -> {
              originalCodeContainer.stop();
              transformedCodeContainer.stop();
            });
  }
}
