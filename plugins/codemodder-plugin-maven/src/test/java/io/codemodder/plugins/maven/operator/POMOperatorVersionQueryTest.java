package io.codemodder.plugins.maven.operator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.xml.stream.XMLStreamException;
import org.dom4j.DocumentException;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class POMOperatorVersionQueryTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(POMOperatorVersionQueryTest.class);

  @Test
  void queryVersions_reads_version_from_mavenCompilerProperties()
      throws XMLStreamException, DocumentException, IOException, URISyntaxException {
    String pomFile = "pom-1.xml";

    Optional<VersionQueryResponse> optionalVersionQueryResponse = getPomFileVersionsQuery(pomFile);

    LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

    VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

    Assert.assertTrue(
        "Version defined is 1.8 as source", versionQueryResponse.getSource().satisfies("=1.8.0"));
    Assert.assertTrue(
        "Version defined is 1.8 as target", versionQueryResponse.getTarget().satisfies("=1.8.0"));
  }

  @Test
  void queryVersions_handles_no_version()
      throws XMLStreamException, DocumentException, IOException, URISyntaxException {

    Optional<VersionQueryResponse> optionalVersionResponse =
        getPomFileVersionsQuery("pom-version-0.xml");

    Assert.assertFalse(
        "No versions defined (queryType: " + QueryType.SAFE + ")",
        optionalVersionResponse.isPresent());
  }

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

              Assert.assertTrue(
                  "Version defined is 1.8 as source",
                  versionQueryResponse.getSource().satisfies("=1.8.0"));
              Assert.assertTrue(
                  "Version defined is 1.8 as target",
                  versionQueryResponse.getTarget().satisfies("=1.8.0"));
            });
  }

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

              Assert.assertTrue(
                  "Version defined is 1.8 as source",
                  versionQueryResponse.getSource().satisfies("=1.8.0"));
              Assert.assertTrue(
                  "Version defined is 1.8 as target",
                  versionQueryResponse.getTarget().satisfies("=1.8.0"));
            });
  }

  @Test
  void queryVersions_reads_version_from_mavenCompilerRelease()
      throws XMLStreamException, DocumentException, IOException, URISyntaxException {
    String pomFile = "pom-version-3.xml";
    LOGGER.info("Using file: " + pomFile);

    Optional<VersionQueryResponse> optionalVersionQueryResponse = getPomFileVersionsQuery(pomFile);

    LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

    VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

    Assert.assertTrue("Version defined is 9", versionQueryResponse.getSource().satisfies("=9.0.0"));
    Assert.assertTrue("Version defined is 9", versionQueryResponse.getTarget().satisfies("=9.0.0"));
  }

  @Test
  void queryVersions_source_and_target_versions_mismatch()
      throws XMLStreamException, DocumentException, IOException, URISyntaxException {
    String pomFile = "pom-version-7.xml";

    Optional<VersionQueryResponse> optionalVersionQueryResponse = getPomFileVersionsQuery(pomFile);

    LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

    VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

    Assert.assertTrue(
        "Version defined is 1.7 as source", versionQueryResponse.getSource().satisfies("=1.7.0"));
    Assert.assertTrue(
        "Version defined is 1.8 as target", versionQueryResponse.getTarget().satisfies("=1.8.0"));
  }

  Optional<VersionQueryResponse> getPomFileVersionsQuery(String pomFile)
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    ProjectModel context =
        ProjectModelFactory.load(this.getClass().getResource(pomFile)).withSafeQueryType().build();

    return POMOperator.queryVersions(context);
  }
}
