package io.codemodder.plugins.maven.operator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
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
  void testBasicQuery() {
    String pomFile = "pom-1.xml";

    Arrays.stream(QueryType.values())
        .filter(queryType -> queryType != QueryType.NONE)
        .forEach(
            queryType -> {
              Optional<VersionQueryResponse> optionalVersionQueryResponse = null;
              try {
                optionalVersionQueryResponse = versionDefinitions(pomFile, queryType, false);
              } catch (DocumentException e) {
                throw new RuntimeException(e);
              } catch (IOException e) {
                throw new RuntimeException(e);
              } catch (URISyntaxException e) {
                throw new RuntimeException(e);
              } catch (XMLStreamException e) {
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
  void testPomVersionZero() {
    Arrays.stream(QueryType.values())
        .filter(queryType -> queryType != QueryType.NONE)
        .forEach(
            queryType -> {
              Optional<VersionQueryResponse> optionalVersionResponse = null;
              try {
                optionalVersionResponse = versionDefinitions("pom-version-0.xml", queryType, false);
              } catch (DocumentException e) {
                throw new RuntimeException(e);
              } catch (IOException e) {
                throw new RuntimeException(e);
              } catch (URISyntaxException e) {
                throw new RuntimeException(e);
              } catch (XMLStreamException e) {
                throw new RuntimeException(e);
              }
              Assert.assertFalse(
                  "No versions defined (queryType: " + queryType + ")",
                  optionalVersionResponse.isPresent());
            });
  }

  @Test
  void testPomVersion1and2() {
    IntStream.rangeClosed(1, 2)
        .forEach(
            index -> {
              String pomFile = "pom-version-" + index + ".xml";
              LOGGER.info("Using file: " + pomFile);

              Arrays.stream(QueryType.values())
                  .filter(queryType -> queryType != QueryType.NONE)
                  .forEach(
                      queryType -> {
                        LOGGER.info("using queryType: " + queryType);

                        Optional<VersionQueryResponse> optionalVersionQueryResponse = null;
                        try {
                          optionalVersionQueryResponse =
                              versionDefinitions(pomFile, queryType, false);
                        } catch (DocumentException e) {
                          throw new RuntimeException(e);
                        } catch (IOException e) {
                          throw new RuntimeException(e);
                        } catch (URISyntaxException e) {
                          throw new RuntimeException(e);
                        } catch (XMLStreamException e) {
                          throw new RuntimeException(e);
                        }

                        LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

                        VersionQueryResponse versionQueryResponse =
                            optionalVersionQueryResponse.get();

                        Assert.assertTrue(
                            "Version defined is 1.8 as source",
                            versionQueryResponse.getSource().satisfies("=1.8.0"));
                        Assert.assertTrue(
                            "Version defined is 1.8 as target",
                            versionQueryResponse.getTarget().satisfies("=1.8.0"));
                      });
            });
  }

  @Test
  void testPomVersion4and5and6Offline() {
    IntStream.rangeClosed(4, 6)
        .forEach(
            index -> {
              String pomFile = "pom-version-" + index + ".xml";
              LOGGER.info("Using file: " + pomFile);

              Arrays.stream(QueryType.values())
                  .filter(queryType -> queryType != QueryType.NONE)
                  .forEach(
                      queryType -> {
                        LOGGER.info("using queryType: " + queryType);

                        Optional<VersionQueryResponse> optionalVersionQueryResponse = null;
                        try {
                          optionalVersionQueryResponse =
                              versionDefinitions(pomFile, queryType, true);
                        } catch (DocumentException e) {
                          throw new RuntimeException(e);
                        } catch (IOException e) {
                          throw new RuntimeException(e);
                        } catch (URISyntaxException e) {
                          throw new RuntimeException(e);
                        } catch (XMLStreamException e) {
                          throw new RuntimeException(e);
                        }

                        LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

                        VersionQueryResponse versionQueryResponse =
                            optionalVersionQueryResponse.get();

                        Assert.assertTrue(
                            "Version defined is 1.8 as source",
                            versionQueryResponse.getSource().satisfies("=1.8.0"));
                        Assert.assertTrue(
                            "Version defined is 1.8 as target",
                            versionQueryResponse.getTarget().satisfies("=1.8.0"));
                      });
            });
  }

  @Test
  void testPomVersion3() {
    String pomFile = "pom-version-3.xml";
    LOGGER.info("Using file: " + pomFile);

    Arrays.stream(QueryType.values())
        .filter(queryType -> queryType != QueryType.NONE)
        .forEach(
            queryType -> {
              LOGGER.info("using queryType: " + queryType);

              Optional<VersionQueryResponse> optionalVersionQueryResponse = null;
              try {
                optionalVersionQueryResponse = versionDefinitions(pomFile, queryType, false);
              } catch (DocumentException e) {
                throw new RuntimeException(e);
              } catch (IOException e) {
                throw new RuntimeException(e);
              } catch (URISyntaxException e) {
                throw new RuntimeException(e);
              } catch (XMLStreamException e) {
                throw new RuntimeException(e);
              }

              LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

              VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

              Assert.assertTrue(
                  "Version defined is 9", versionQueryResponse.getSource().satisfies("=9.0.0"));
              Assert.assertTrue(
                  "Version defined is 9", versionQueryResponse.getTarget().satisfies("=9.0.0"));
            });
  }

  @Test
  void testPomVersionsMismatching() {
    String pomFile = "pom-version-7.xml";

    Arrays.stream(QueryType.values())
        .filter(queryType -> queryType != QueryType.NONE)
        .forEach(
            queryType -> {
              Optional<VersionQueryResponse> optionalVersionQueryResponse = null;
              try {
                optionalVersionQueryResponse = versionDefinitions(pomFile, queryType, false);
              } catch (DocumentException e) {
                throw new RuntimeException(e);
              } catch (IOException e) {
                throw new RuntimeException(e);
              } catch (URISyntaxException e) {
                throw new RuntimeException(e);
              } catch (XMLStreamException e) {
                throw new RuntimeException(e);
              }

              LOGGER.debug("Versions found: {}", optionalVersionQueryResponse);

              VersionQueryResponse versionQueryResponse = optionalVersionQueryResponse.get();

              Assert.assertTrue(
                  "Version defined is 1.7 as source",
                  versionQueryResponse.getSource().satisfies("=1.7.0"));
              Assert.assertTrue(
                  "Version defined is 1.8 as target",
                  versionQueryResponse.getTarget().satisfies("=1.8.0"));
            });
  }

  Optional<VersionQueryResponse> versionDefinitions(
      String pomFile, QueryType queryType, boolean offline)
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    ProjectModel context =
        ProjectModelFactory.load(this.getClass().getResource(pomFile))
            .withQueryType(queryType)
            .withOffline(offline)
            .build();

    return POMOperator.queryVersions(context);
  }
}
