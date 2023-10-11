package io.codemodder.plugins.maven.operator;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.stream.XMLStreamException;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class POMOperatorDependencyQueryTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(POMOperatorTest.class);

  @Test
  void testBasicQuery()
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    for (QueryType queryType : QueryType.values()) {
      if (queryType != QueryType.NONE) {
        ProjectModelFactory context = ProjectModelFactory.load(getClass().getResource("pom-1.xml"));
        context.withQueryType(queryType);

        Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

        LOGGER.debug("Dependencies found: {}", dependencies);

        assertTrue("Dependencies are not empty", !dependencies.isEmpty());
      }
    }
  }

  @Test
  void testFailedSafeQuery()
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    ProjectModelFactory context =
        ProjectModelFactory.load(getClass().getResource("pom-broken.xml"));
    context.withQueryType(QueryType.SAFE);

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    assertTrue("Dependencies are empty", dependencies.isEmpty());
  }

  @Test
  void testFailedUnsafeQuery()
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    Assertions.assertThrows(
        IllegalStateException.class,
        () -> {
          ProjectModelFactory context =
              ProjectModelFactory.load(getClass().getResource("pom-broken.xml"));
          context.withQueryType(QueryType.UNSAFE);

          Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

          assertTrue("Dependencies are empty", dependencies.isEmpty());
        });
  }

  @Test
  void testAllQueryTypes()
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    String[] pomFiles = {"pom-1.xml", "pom-3.xml"};
    for (String pomFile : pomFiles) {
      for (Pair<QueryType, String> chain : Chain.AVAILABLE_DEPENDENCY_QUERY_COMMANDS) {
        String commandClassName = "io.codemodder.plugins.maven.operator." + chain.getSecond();

        List<Command> commandListOverride = new ArrayList<>();
        try {
          Command command = (Command) Class.forName(commandClassName).newInstance();
          commandListOverride.add(command);
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
          e.printStackTrace();
        }

        ProjectModelFactory context = ProjectModelFactory.load(getClass().getResource(pomFile));
        context.withQueryType(QueryType.UNSAFE);

        Collection<Dependency> dependencies =
            POMOperator.queryDependency(context.build(), commandListOverride);

        assertTrue("Dependencies are not empty", !dependencies.isEmpty());
      }
    }
  }

  @Test
  void testTemporaryDirectory()
      throws IOException, DocumentException, URISyntaxException, XMLStreamException {
    for (QueryType queryType : QueryType.values()) {
      if (queryType != QueryType.NONE) {
        File tempDirectory = new File("/tmp/mvn-repo-" + System.currentTimeMillis() + ".dir");

        LOGGER.info("Using queryType: " + queryType + " at " + tempDirectory);

        assertFalse("Temp Directory does not exist initially", tempDirectory.exists());
        assertEquals(
            "There must be no files",
            tempDirectory.list() != null
                ? (int) Files.list(tempDirectory.toPath()).filter(Files::isDirectory).count()
                : 0,
            0);

        ProjectModelFactory context = ProjectModelFactory.load(getClass().getResource("pom-1.xml"));
        context.withQueryType(queryType);
        context.withRepositoryPath(tempDirectory);

        Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

        LOGGER.debug("Dependencies found: " + dependencies);

        assertTrue("Dependencies are not empty", !dependencies.isEmpty());

        assertTrue("Temp Directory ends up existing", tempDirectory.exists());
        assertTrue("Temp Directory is a directory", tempDirectory.isDirectory());
      }
    }
  }

  @Test
  void testTemporaryDirectoryAndFullyOffline()
      throws IOException, DocumentException, URISyntaxException, XMLStreamException {
    for (QueryType queryType : QueryType.values()) {
      if (queryType != QueryType.NONE && queryType == QueryType.SAFE) {
        File tempDirectory = Files.createTempDirectory("mvn-repo").toFile();

        ProjectModelFactory context = ProjectModelFactory.load(getClass().getResource("pom-1.xml"));
        context.withQueryType(queryType);
        context.withRepositoryPath(tempDirectory);
        context.withOffline(true);

        Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

        LOGGER.debug("Dependencies found: " + dependencies);

        assertTrue("Dependencies are not empty", !dependencies.isEmpty());
      }
    }
  }

  @Test
  void testOnSyntheticDependency() throws Exception {
    File tempDirectory = Files.createTempDirectory("mvn-repo").toFile();

    Path tempPom = new File(tempDirectory, "pom.xml").toPath();

    String randomName = "random-artifact-" + System.currentTimeMillis();

    Files.write(
        tempPom,
        String.join(
                "\n",
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                "<project",
                "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
                "        xmlns=\"http://maven.apache.org/POM/4.0.0\"",
                "        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
                "    <modelVersion>4.0.0</modelVersion>",
                "",
                "    <groupId>br.com.ingenieux</groupId>",
                "    <artifactId>pom-operator</artifactId>",
                "    <version>0.0.1-SNAPSHOT</version>",
                "",
                "    <dependencies>",
                "        <dependency>",
                "            <groupId>dummyorg</groupId>",
                "            <artifactId>" + randomName + "</artifactId>",
                "            <version>2.1.3</version>",
                "        </dependency>",
                "    </dependencies>",
                "</project>")
            .getBytes());

    ProjectModelFactory context = ProjectModelFactory.load(tempPom.toFile());
    context.withQueryType(QueryType.SAFE);
    context.withRepositoryPath(tempDirectory);
    context.withOffline(true);

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: " + dependencies);

    assertTrue("Dependencies are not empty", !dependencies.isEmpty());

    assertTrue(
        "Random name matches",
        dependencies.stream()
            .collect(Collectors.toList())
            .get(0)
            .getArtifactId()
            .equals(randomName));
  }

  @Test
  void testOnCompositeSyntheticDependency() throws Exception {
    File tempDirectory = Files.createTempDirectory("mvn-repo").toFile();

    Path tempParentPom = new File(tempDirectory, "pom-parent.xml").toPath();
    Path tempPom = new File(tempDirectory, "pom.xml").toPath();

    String randomName = "random-artifact-" + System.currentTimeMillis();

    String parentPomContent =
        String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<project",
            "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
            "        xmlns=\"http://maven.apache.org/POM/4.0.0\"",
            "        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
            "    <modelVersion>4.0.0</modelVersion>",
            "",
            "    <artifactId>somethingelse</artifactId>",
            "    <groupId>br.com.ingenieux</groupId>",
            "    <version>1</version>",
            "    <packaging>pom</packaging>",
            "    <dependencyManagement>",
            "        <dependencies>",
            "            <dependency>",
            "                <groupId>dummyorg</groupId>",
            "                <artifactId>managed-" + randomName + "</artifactId>",
            "                <version>1</version>",
            "            </dependency>",
            "        </dependencies>",
            "    </dependencyManagement>",
            "</project>");

    String pomContent =
        String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<project",
            "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
            "        xmlns=\"http://maven.apache.org/POM/4.0.0\"",
            "        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
            "    <modelVersion>4.0.0</modelVersion>",
            "",
            "    <groupId>br.com.ingenieux</groupId>",
            "    <artifactId>pom-operator</artifactId>",
            "    <version>0.0.1-SNAPSHOT</version>",
            "    <parent>",
            "      <artifactId>somethingelse</artifactId>",
            "      <groupId>br.com.ingenieux</groupId>",
            "      <version>1</version>",
            "      <relativePath>./pom-parent.xml</relativePath>",
            "    </parent>",
            "    <dependencies>",
            "        <dependency>",
            "            <groupId>dummyorg</groupId>",
            "            <artifactId>" + randomName + "</artifactId>",
            "            <version>2.1.3</version>",
            "        </dependency>",
            "        <dependency>",
            "            <groupId>dummyorg</groupId>",
            "            <artifactId>managed-" + randomName + "</artifactId>",
            "        </dependency>",
            "    </dependencies>",
            "</project>");

    Files.write(tempParentPom, parentPomContent.getBytes());
    Files.write(tempPom, pomContent.getBytes());

    ProjectModelFactory context = ProjectModelFactory.load(tempPom.toFile());
    context.withQueryType(QueryType.SAFE);
    context.withRepositoryPath(tempDirectory);
    context.withOffline(true);

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: " + dependencies);

    assertTrue("Dependencies are not empty", !dependencies.isEmpty());

    assertTrue(
        "Random name matches",
        dependencies.stream()
            .collect(Collectors.toList())
            .get(0)
            .getArtifactId()
            .equals(randomName));
  }

  @Test
  void testOnCompositeSyntheticDependencyIncompleteWithoutParsing() throws Exception {
    File tempDirectory = Files.createTempDirectory("mvn-repo").toFile();
    Path tempPom = new File(tempDirectory, "pom.xml").toPath();
    String randomName = "random-artifact-" + System.currentTimeMillis();

    String pomContent =
        String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<project",
            "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
            "        xmlns=\"http://maven.apache.org/POM/4.0.0\"",
            "        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
            "    <modelVersion>4.0.0</modelVersion>",
            "",
            "    <groupId>br.com.ingenieux</groupId>",
            "    <artifactId>pom-operator</artifactId>",
            "    <version>0.0.1-SNAPSHOT</version>",
            "    <parent>",
            "      <artifactId>somethingelse</artifactId>",
            "      <groupId>br.com.ingenieux</groupId>",
            "      <version>1</version>",
            "      <relativePath>./pom-parent.xml</relativePath>",
            "    </parent>",
            "    <dependencies>",
            "        <dependency>",
            "            <groupId>dummyorg</groupId>",
            "            <artifactId>" + randomName + "</artifactId>",
            "            <version>2.1.3</version>",
            "        </dependency>",
            "        <dependency>",
            "            <groupId>dummyorg</groupId>",
            "            <artifactId>managed-" + randomName + "</artifactId>",
            "        </dependency>",
            "    </dependencies>",
            "</project>");

    Files.write(tempPom, pomContent.getBytes());

    ProjectModelFactory context = ProjectModelFactory.load(tempPom.toFile());
    context.withQueryType(QueryType.SAFE);
    context.withRepositoryPath(tempDirectory);
    context.withOffline(true);

    List<Command> commandList = getCommandListFor("QueryByEmbedder", "QueryByResolver");
    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build(), commandList);

    LOGGER.debug("Dependencies found: {}", dependencies);

    assertTrue("Dependencies are empty", dependencies.isEmpty());
  }

  @Test
  void testOnCompositeSyntheticDependencyIncompleteButWithParser() throws Exception {
    File tempDirectory = Files.createTempDirectory("mvn-repo").toFile();
    Path tempPom = new File(tempDirectory, "pom.xml").toPath();
    String randomName = "random-artifact-" + System.currentTimeMillis();

    String pomContent =
        String.join(
            "\n",
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
            "<project",
            "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"",
            "        xmlns=\"http://maven.apache.org/POM/4.0.0\"",
            "        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">",
            "    <modelVersion>4.0.0</modelVersion>",
            "",
            "    <groupId>br.com.ingenieux</groupId>",
            "    <artifactId>pom-operator</artifactId>",
            "    <version>0.0.1-SNAPSHOT</version>",
            "    <parent>",
            "      <artifactId>somethingelse</artifactId>",
            "      <groupId>br.com.ingenieux</groupId>",
            "      <version>1</version>",
            "      <relativePath>./pom-parent.xml</relativePath>",
            "    </parent>",
            "    <dependencyManagement>",
            "        <dependencies>",
            "            <dependency>",
            "                <groupId>dummyorg</groupId>",
            "                <artifactId>managed-" + randomName + "</artifactId>",
            "                <version>0.0.1</version>",
            "            </dependency>",
            "        </dependencies>",
            "    </dependencyManagement>",
            "    <dependencies>",
            "        <dependency>",
            "            <groupId>dummyorg</groupId>",
            "            <artifactId>" + randomName + "</artifactId>",
            "            <version>2.1.3</version>",
            "        </dependency>",
            "        <dependency>",
            "            <groupId>dummyorg</groupId>",
            "            <artifactId>managed-" + randomName + "</artifactId>",
            "        </dependency>",
            "    </dependencies>",
            "</project>");

    Files.write(tempPom, pomContent.getBytes());

    ProjectModelFactory context = ProjectModelFactory.load(tempPom.toFile());
    context.withQueryType(QueryType.SAFE);
    context.withRepositoryPath(tempDirectory);
    context.withOffline(true);

    List<Command> commandList = getCommandListFor("QueryByParsing");
    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build(), commandList);

    LOGGER.debug("Dependencies found: {}", dependencies);

    assertTrue("Dependencies are empty", !dependencies.isEmpty());

    Dependency foundDependency =
        dependencies.stream()
            .filter(
                dep ->
                    "0.0.1".equals(dep.getVersion())
                        && ("managed-" + randomName).equals(dep.getArtifactId()))
            .findAny()
            .orElse(null);

    assertTrue("There's a dependency with managed-version", foundDependency != null);
  }

  private List<Command> getCommandListFor(String... names) {
    List<Command> commandList = new ArrayList<>();

    for (String name : names) {
      String commandClassName = "io.codemodder.plugins.maven.operator." + name;

      try {
        Class<?> commandClass = Class.forName(commandClassName);
        Command commandInstance = (Command) commandClass.newInstance();
        commandList.add(commandInstance);
      } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
        e.printStackTrace();
      }
    }

    return commandList;
  }

  @Test
  void testOfflineQueryResolution() throws Exception {
    File tempDirectory = Files.createTempDirectory("mvn-repo").toFile();
    File pomFile = new File(getClass().getResource("nested/child/pom/pom-3-child.xml").getFile());

    ProjectModelFactory context = ProjectModelFactory.load(pomFile);
    context.withQueryType(QueryType.SAFE);
    context.withRepositoryPath(tempDirectory);
    context.withOffline(true);

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: {}", dependencies);

    assertTrue("Dependencies are empty", dependencies.isEmpty());
  }
}
