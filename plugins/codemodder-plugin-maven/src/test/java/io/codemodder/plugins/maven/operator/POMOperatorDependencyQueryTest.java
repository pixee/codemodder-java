package io.codemodder.plugins.maven.operator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class POMOperatorDependencyQueryTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(POMOperatorTest.class);

  /**
   * Tests whether queryDependency uses the safe query type and retrieves dependencies successfully.
   * Ensures that dependencies are retrieved when a safe query type is used.
   */
  @Test
  void queryDependency_uses_safe_query_type_successfully()
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    ProjectModelFactory context = ProjectModelFactory.load(getClass().getResource("pom-1.xml"));
    context.withSafeQueryType();

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: {}", dependencies);

    // "Dependencies are not empty"
    assertThat(dependencies != null && !dependencies.isEmpty()).isTrue();
  }

  /**
   * Tests queryDependency for a broken POM, expecting no dependencies. Verifies that the query for
   * a broken POM returns an empty list of dependencies.
   */
  @Test
  void queryDependency_for_broken_pom_returns_no_dependencies()
      throws DocumentException, IOException, URISyntaxException, XMLStreamException {
    ProjectModelFactory context =
        ProjectModelFactory.load(getClass().getResource("pom-broken.xml"));
    context.withSafeQueryType();

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    // "Dependencies are empty"
    assertThat(dependencies).isEmpty();
  }

  /**
   * Tests whether queryDependency uses available dependency query commands successfully. Verifies
   * that the query uses available dependency query commands and returns non-empty dependencies.
   */
  @Test
  void queryDependency_uses_available_dependency_query_commands_successfully()
      throws DocumentException,
          IOException,
          URISyntaxException,
          XMLStreamException,
          ClassNotFoundException,
          InstantiationException,
          IllegalAccessException {
    String[] pomFiles = {"pom-1.xml", "pom-3.xml"};
    for (String pomFile : pomFiles) {
      for (Pair<QueryType, String> chain : CommandChain.AVAILABLE_DEPENDENCY_QUERY_COMMANDS) {
        String commandClassName = "io.codemodder.plugins.maven.operator." + chain.getSecond();

        List<Command> commandListOverride = new ArrayList<>();

        Command command = (Command) Class.forName(commandClassName).newInstance();
        commandListOverride.add(command);

        ProjectModelFactory context = ProjectModelFactory.load(getClass().getResource(pomFile));
        context.withSafeQueryType();

        Collection<Dependency> dependencies =
            POMOperator.queryDependency(context.build(), commandListOverride);

        // "Dependencies are not empty"
        assertThat(!dependencies.isEmpty()).isTrue();
      }
    }
  }

  /**
   * Tests whether queryDependency uses a temporary directory successfully. Verifies that the query
   * uses a temporary directory and returns non-empty dependencies.
   */
  @Test
  void queryDependency_uses_temporary_directory_successfully()
      throws IOException, DocumentException, URISyntaxException, XMLStreamException {

    File tempDirectory = new File("/tmp/mvn-repo-" + System.currentTimeMillis() + ".dir");

    // "Temp Directory does not exist initially"
    assertThat(tempDirectory).doesNotExist();
    // "There must be no files"
    assertThat(
            tempDirectory.list() != null
                ? (int) Files.list(tempDirectory.toPath()).filter(Files::isDirectory).count()
                : 0)
        .isZero();

    ProjectModelFactory context = ProjectModelFactory.load(getClass().getResource("pom-1.xml"));
    context.withSafeQueryType();
    context.withRepositoryPath(tempDirectory.toPath());

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: " + dependencies);

    // "Dependencies are not empty"
    assertThat(dependencies != null && !dependencies.isEmpty()).isTrue();

    // "Temp Directory ends up existing"
    assertThat(tempDirectory).exists();
    // "Temp Directory is a directory"
    assertThat(tempDirectory).isDirectory();
  }

  /**
   * Tests whether queryDependency uses a temporary directory and offline mode successfully.
   * Verifies that the query uses a temporary directory and offline mode, returning non-empty
   * dependencies.
   */
  @Test
  void queryDependency_uses_temporary_directory_and_offline_mode_successfully()
      throws IOException, DocumentException, URISyntaxException, XMLStreamException {

    Path tempDirectory = Files.createTempDirectory("mvn-repo");

    ProjectModelFactory context = ProjectModelFactory.load(getClass().getResource("pom-1.xml"));
    context.withSafeQueryType();
    context.withRepositoryPath(tempDirectory);

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: " + dependencies);

    // "Dependencies are not empty"
    assertThat(!dependencies.isEmpty()).isTrue();
  }

  /**
   * Tests whether queryDependency handles a synthetic dependency. Verifies the handling of
   * synthetic dependencies in the query.
   */
  @Test
  void queryDependency_handles_synthetic_dependency() throws Exception {
    Path tempDirectory = Files.createTempDirectory("mvn-repo");

    Path tempPom = tempDirectory.resolve("pom.xml");

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

    ProjectModelFactory context = ProjectModelFactory.load(tempPom);
    context.withSafeQueryType();
    context.withRepositoryPath(tempDirectory);

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: " + dependencies);

    // "Dependencies are not empty"
    assertThat(!dependencies.isEmpty()).isTrue();

    // "Random name matches"
    assertThat(dependencies.stream().toList().get(0).getArtifactId()).isEqualTo(randomName);
  }

  /**
   * Tests queryDependency handles a composite synthetic dependency. Verifies the handling of
   * composite synthetic dependencies in the query.
   */
  @Test
  void queryDependency_handles_composite_synthetic_dependency() throws Exception {
    Path tempDirectory = Files.createTempDirectory("mvn-repo");

    Path tempParentPom = tempDirectory.resolve("pom-parent.xml");
    Path tempPom = tempDirectory.resolve("pom.xml");

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

    ProjectModelFactory context = ProjectModelFactory.load(tempPom);
    context.withSafeQueryType();
    context.withRepositoryPath(tempDirectory);

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: " + dependencies);

    // "Dependencies are not empty"
    assertThat(!dependencies.isEmpty()).isTrue();

    // "Random name matches"
    assertThat(dependencies.stream().toList().get(0).getArtifactId()).isEqualTo(randomName);
  }

  /**
   * Tests queryDependency handles composite incomplete synthetic dependency but with a parser.
   * Verifies the handling of composite incomplete synthetic dependencies with a parser in the
   * query.
   */
  @Test
  void queryDependency_handles_composite_incomplete_synthetic_dependency_but_with_parser()
      throws Exception {
    Path tempDirectory = Files.createTempDirectory("mvn-repo");
    Path tempPom = tempDirectory.resolve("pom.xml");
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

    ProjectModelFactory context = ProjectModelFactory.load(tempPom);
    context.withSafeQueryType();
    context.withRepositoryPath(tempDirectory);

    List<Command> commandList = getCommandListFor("QueryByParsing");
    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build(), commandList);

    LOGGER.debug("Dependencies found: {}", dependencies);

    // "Dependencies are empty"
    assertThat(!dependencies.isEmpty()).isTrue();

    Dependency foundDependency =
        dependencies.stream()
            .filter(
                dep ->
                    "0.0.1".equals(dep.getVersion())
                        && ("managed-" + randomName).equals(dep.getArtifactId()))
            .findAny()
            .orElse(null);

    // "There's a dependency with managed-version",
    assertThat(foundDependency).isNotNull();
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

  /**
   * Tests queryDependency handles offline mode. Verifies the behavior of dependency retrieval in
   * offline mode.
   */
  @Test
  void queryDependency_handles_offline_mode() throws Exception {
    Path tempDirectory = Files.createTempDirectory("mvn-repo");
    Path pomFilePath =
        Paths.get(getClass().getResource("nested/child/pom/pom-3-child.xml").toURI());

    ProjectModelFactory context = ProjectModelFactory.load(pomFilePath);
    context.withSafeQueryType();
    context.withRepositoryPath(tempDirectory);

    Collection<Dependency> dependencies = POMOperator.queryDependency(context.build());

    LOGGER.debug("Dependencies found: {}", dependencies);

    // "Dependencies are empty"
    assertThat(dependencies).isEmpty();
  }
}
