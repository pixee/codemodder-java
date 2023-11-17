package io.codemodder.plugins.maven.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.xml.stream.XMLStreamException;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class POMOperatorVersionQueryTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(POMOperatorVersionQueryTest.class);

  /**
   * Tests querying versions from mavenCompilerProperties in a POM file.
   *
   * <p>Verifies if the versions are properly read from mavenCompilerProperties in the specified POM
   * file.
   */
  @Test
  void queryVersions_reads_version_from_mavenCompilerProperties()
      throws XMLStreamException, DocumentException, IOException, URISyntaxException {
    String pomFile = "pom-1.xml";

    Optional<VersionQueryResponse> optionalVersionQueryResponse = getPomFileVersionsQuery(pomFile);

    LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

    VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

    assertThat(versionQueryResponse.getSource().satisfies("=1.8.0")).isTrue();
    assertThat(versionQueryResponse.getTarget().satisfies("=1.8.0")).isTrue();
  }

  /**
   * Tests scenario when no versions are defined in the POM file.
   *
   * <p>Ensures handling of scenarios where no versions are defined in the POM file using SAFE
   * query.
   */
  @Test
  void queryVersions_handles_no_version()
      throws XMLStreamException, DocumentException, IOException, URISyntaxException {

    Optional<VersionQueryResponse> optionalVersionResponse =
        getPomFileVersionsQuery("pom-version-0.xml");

    assertThat(optionalVersionResponse).isNotPresent();
  }

  /**
   * Tests the functionality to read versions from the Maven Plugin configuration. This test ensures
   * that the system correctly retrieves and verifies versions specified within Maven Plugin
   * configurations.
   */
  @Test
  void queryVersions_reads_version_from_mavenPlugin() {
    IntStream.rangeClosed(1, 2)
        .forEach(
            index -> {
              String pomFile = "pom-version-" + index + ".xml";
              LOGGER.info("Using file: " + pomFile);

              Optional<VersionQueryResponse> optionalVersionQueryResponse = null;
              try {
                optionalVersionQueryResponse = getPomFileVersionsQuery(pomFile);
              } catch (DocumentException
                  | IOException
                  | URISyntaxException
                  | XMLStreamException e) {
                throw new RuntimeException(e);
              }

              LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

              VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

              assertThat(versionQueryResponse.getSource().satisfies("=1.8.0")).isTrue();
              assertThat(versionQueryResponse.getTarget().satisfies("=1.8.0")).isTrue();
            });
  }

  /**
   * Tests the functionality to read versions from the Maven Plugin configuration using a safe query
   * type. This test ensures that the system correctly handles safe queries while extracting
   * versions from Maven Plugin configurations.
   */
  @Test
  void queryVersions_reads_version_from_mavenPlugin_usingSafeQuery() {
    IntStream.rangeClosed(4, 6)
        .forEach(
            index -> {
              String pomFile = "pom-version-" + index + ".xml";
              LOGGER.info("Using file: " + pomFile);

              Optional<VersionQueryResponse> optionalVersionQueryResponse = null;
              try {
                optionalVersionQueryResponse = getPomFileVersionsQuery(pomFile);
              } catch (DocumentException
                  | IOException
                  | URISyntaxException
                  | XMLStreamException e) {
                throw new RuntimeException(e);
              }

              LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

              VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

              assertThat(versionQueryResponse.getSource().satisfies("=1.8.0")).isTrue();
              assertThat(versionQueryResponse.getTarget().satisfies("=1.8.0")).isTrue();
            });
  }

  /**
   * Tests the functionality to read versions from the Maven Compiler Release. This test ensures
   * that the system accurately extracts and validates versions specified within the Maven Compiler
   * Release configuration.
   */
  @Test
  void queryVersions_reads_version_from_mavenCompilerRelease()
      throws XMLStreamException, DocumentException, IOException, URISyntaxException {
    String pomFile = "pom-version-3.xml";
    LOGGER.info("Using file: " + pomFile);

    Optional<VersionQueryResponse> optionalVersionQueryResponse = getPomFileVersionsQuery(pomFile);

    LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

    VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

    assertThat(versionQueryResponse.getSource().satisfies("=9.0.0")).isTrue();
    assertThat(versionQueryResponse.getTarget().satisfies("=9.0.0")).isTrue();
  }

  /**
   * Tests scenarios where source and target versions mismatch. This test ensures that the system
   * correctly identifies cases where the source and target versions specified in the configuration
   * files do not match.
   */
  @Test
  void queryVersions_source_and_target_versions_mismatch()
      throws XMLStreamException, DocumentException, IOException, URISyntaxException {
    String pomFile = "pom-version-7.xml";

    Optional<VersionQueryResponse> optionalVersionQueryResponse = getPomFileVersionsQuery(pomFile);

    LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

    VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

    assertThat(versionQueryResponse.getSource().satisfies("=1.7.0")).isTrue();
    assertThat(versionQueryResponse.getTarget().satisfies("=1.8.0")).isTrue();
  }

  Optional<VersionQueryResponse> getPomFileVersionsQuery(String pomFile)
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    ProjectModel context =
        ProjectModelFactory.load(this.getClass().getResource(pomFile)).withSafeQueryType().build();

    return POMOperator.queryVersions(context);
  }
}
