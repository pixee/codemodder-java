package io.codemodder.plugins.maven.operator;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.apache.commons.lang3.SystemUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.diff.ComparisonType;
import org.xmlunit.diff.Diff;
import org.xmlunit.diff.Difference;

final class POMOperatorTest extends AbstractTestBase {

  private static final Logger LOGGER = LoggerFactory.getLogger(POMOperatorTest.class);

  /**
   * Tests the behavior with a broken POM.
   *
   * <p>Expects a {@code DocumentException} to be thrown when attempting to modify a broken POM.
   */
  @Test
  void modify_expects_document_exception_for_broken_pom() {
    Assertions.assertThrows(
        DocumentException.class,
        () -> {
          performAndAssertModifyPomOperation(
              "broken-pom",
              ProjectModelFactory.load(POMOperatorTest.class.getResource("broken-pom.xml"))
                  .withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3")));
        });
  }

  /**
   * Tests modification with multiple dependencies and validates the resultant POM.
   *
   * <p>This test adds multiple dependencies to a POM, writes it to a temporary file, performs
   * modifications, and validates the resulting POM's contents and structure.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_with_multiples_dependencies_successfully() throws Exception {
    List<Dependency> deps = new ArrayList<>();
    deps.add(Dependency.fromString("org.slf4j:slf4j-api:1.7.25"));
    deps.add(Dependency.fromString("io.github.pixee:java-code-security-toolkit:1.0.2"));
    deps.add(Dependency.fromString("org.owasp.encoder:encoder:1.2.3"));

    Path testPomPath = Files.createTempFile("pom", ".xml");

    try (InputStream inputStream = POMOperatorTest.class.getResourceAsStream("sample-bad-pom.xml");
        OutputStream outputStream = Files.newOutputStream(testPomPath)) {
      byte[] buffer = new byte[1024];
      int bytesRead;
      while ((bytesRead = inputStream.read(buffer)) != -1) {
        outputStream.write(buffer, 0, bytesRead);
      }
    }

    for (Dependency d : deps) {

      ProjectModel projectModelFactory =
          ProjectModelFactory.load(testPomPath)
              .withDependency(d)
              .withUseProperties(true)
              .withOverrideIfAlreadyExists(true)
              .build();

      if (POMOperator.modify(projectModelFactory)) {
        Assert.assertTrue(
            "Original POM File is Dirty", projectModelFactory.getPomFile().getDirty());

        String resultPomAsXml =
            new String(
                projectModelFactory.getPomFile().getResultPomBytes(), Charset.defaultCharset());

        LOGGER.debug("resultPomAsXml: {}", resultPomAsXml);

        try (OutputStream outputStream = Files.newOutputStream(testPomPath)) {
          outputStream.write(projectModelFactory.getPomFile().getResultPomBytes());
        } catch (IOException e) {
          // Handle the IOException
          e.printStackTrace();
        }
      } else {
        throw new IllegalStateException("Code that shouldn't be reached at all");
      }
    }

    Collection<Dependency> resolvedDeps =
        POMOperator.queryDependency(
            ProjectModelFactory.load(testPomPath).withSafeQueryType().build());

    String testPomContents = new String(Files.readAllBytes(testPomPath), Charset.defaultCharset());

    Assert.assertTrue("Must have three dependencies", 3 == resolvedDeps.size());
    Assert.assertTrue("Must have a comment inside", testPomContents.contains("<!--"));
    Assert.assertTrue(
        "Must have a formatted attribute spanning a whole line inside",
        testPomContents.contains(
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"));
  }

  /**
   * Tests modification with a missing dependency.
   *
   * <p>Expects a {@code MissingDependencyException} to be thrown when attempting to modify a POM
   * with a missing dependency.
   */
  @Test
  void modify_throws_missing_dependency_exception() {
    Assertions.assertThrows(
        MissingDependencyException.class,
        () -> {
          performAndAssertModifyPomOperation(
              "case-dependency-missing",
              ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-case-1.xml")));
        });
  }

  /**
   * Tests a specific scenario (Case One) without any dependency and validates the resultant POM.
   *
   * <p>This test applies modifications related to a specific case and validates the resulting POM
   * structure and content changes.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_adds_dependencies_to_pom_without_dependencies() throws Exception {

    ProjectModelFactory projectModelFactory =
        ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-case-1.xml"))
            .withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3"));
    ProjectModel context = performAndAssertModifyPomOperation("case-1", projectModelFactory);

    Assert.assertTrue("Original POM File is Dirty", context.getPomFile().getDirty());

    Diff diff =
        getXmlDifferences(
            context.getPomFile().getPomDocument(), context.getPomFile().getResultPom());

    Assert.assertTrue("Document has differences", diff.hasDifferences());

    String textDiff =
        (String)
            getTextDifferences(
                context.getPomFile().getPomDocument(), context.getPomFile().getResultPom());

    System.out.println("textDiff: " + textDiff);

    Assert.assertTrue(
        "diff contains a <dependencyManagement> tag", textDiff.contains("<dependencyManagement>"));
    Assert.assertTrue("diff contains a <dependency> tag", textDiff.contains("<dependency>"));
  }

  /**
   * Updates version to existing dependency and validates the resultant POM.
   *
   * <p>This test applies modifications related to a specific case and validates the resulting POM
   * structure and content changes.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_downgrades_dependency_version_successfully() throws Exception {
    Dependency dependencyToUpgradeOnCaseThree =
        new Dependency("org.dom4j", "dom4j", "2.0.2", null, null, null);

    ProjectModelFactory projectModelFactory =
        ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-case-3.xml"))
            .withDependency(dependencyToUpgradeOnCaseThree)
            .withSkipIfNewer(false);
    ProjectModel context = performAndAssertModifyPomOperation("case-3", projectModelFactory);

    Diff diff =
        getXmlDifferences(
            context.getPomFile().getPomDocument(), context.getPomFile().getResultPom());

    Assert.assertTrue("Original POM File is Dirty", context.getPomFile().getDirty());

    Assert.assertTrue("Document has differences", diff.hasDifferences());

    Iterable<Difference> differences = diff.getDifferences();
    List<Difference> differenceList = new ArrayList<>();
    for (Difference difference : differences) {
      differenceList.add(difference);
    }

    Assert.assertEquals("Document has a single difference", 1, differenceList.size());

    Difference difference = diff.getDifferences().iterator().next();
    Assert.assertEquals(
        "Document has different versions",
        ComparisonType.TEXT_VALUE,
        difference.getComparison().getType());
    Assert.assertEquals(
        "Document has changed version set to " + dependencyToUpgradeOnCaseThree.getVersion(),
        dependencyToUpgradeOnCaseThree.getVersion(),
        difference.getComparison().getTestDetails().getValue());
  }

  /**
   * It keeps the same version since it is newer than the one to add
   *
   * <p>This test applies modifications related to a specific case with a lower version and
   * validates the resulting POM structure and content changes.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_does_not_downgrade_version_because_keeps_newer() throws Exception {
    Dependency dependencyToUpgrade =
        new Dependency("org.dom4j", "dom4j", "2.0.2", null, null, null);

    ProjectModel context =
        performAndAssertModifyPomOperation(
            "pom-case-three-with-lower-version",
            ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-case-3.xml"))
                .withDependency(dependencyToUpgrade)
                .withSkipIfNewer(true));

    Diff diff =
        getXmlDifferences(
            context.getPomFile().getPomDocument(), context.getPomFile().getResultPom());

    Assert.assertFalse("Document has no differences", diff.hasDifferences());
    Assert.assertFalse("Original POM File is not Dirty", context.getPomFile().getDirty());
  }

  /**
   * It installs POM file and modify adds a new dependency
   *
   * <p>This test applies modifications related to a specific case and validates the resulting POM
   * structure, content changes, and dependencies.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void installs_pom_and_modify_adds_new_dependency() throws Exception {
    File pomPath = new File(POMOperatorTest.class.getResource("webgoat-parent.xml").toURI());

    List<String> args = new ArrayList<>();
    if (SystemUtils.IS_OS_WINDOWS) {
      args.add("cmd.exe");
      args.add("/c");
    }
    args.add(Util.which("mvn").getAbsolutePath());
    args.add("-N");
    args.add("install:install-file");
    args.add("-DgroupId=org.owasp.webgoat");
    args.add("-DartifactId=webgoat-parent");
    args.add("-Dversion=8.2.3-SNAPSHOT");
    args.add("-Dpackaging=pom");
    args.add("-Dfile=" + pomPath.getAbsolutePath());

    ProcessBuilder processBuilder = new ProcessBuilder(args.toArray(new String[0]));
    int exitCode = processBuilder.start().waitFor();

    Assert.assertEquals("POM install was successful", 0, exitCode);

    Dependency dependencyToUpgrade =
        new Dependency("org.apache.activemq", "activemq-amqp", "5.16.2", null, null, null);

    ProjectModel context =
        performAndAssertModifyPomOperation(
            "case-4",
            ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-case-4.xml"))
                .withDependency(dependencyToUpgrade));

    Diff diff =
        getXmlDifferences(
            context.getPomFile().getPomDocument(), context.getPomFile().getResultPom());

    Assert.assertTrue("Document has differences", diff.hasDifferences());
    Assert.assertTrue("Original POM File is Dirty", context.getPomFile().getDirty());

    Document effectivePom = POMTestHelper.getEffectivePom(context);

    Assert.assertFalse(
        "Dependencies Section did change",
        Util.selectXPathNodes(
                effectivePom, Util.buildLookupExpressionForDependency(dependencyToUpgrade))
            .isEmpty());
  }

  /**
   * Modify adds a dependency in a scenario with an empty element (<email />) and validates the
   * resultant POM structure.
   *
   * <p>This test applies modifications related to an empty element scenario and validates the
   * resulting POM structure.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_adds_dependency_to_pom_with_email_empty_element_successfully() throws Exception {
    Dependency dependencyToUpgrade =
        new Dependency("io.github.pixee", "java-security-toolkit", "1.0.2", null, null, null);

    ProjectModel context =
        performAndAssertModifyPomOperation(
            "case-5",
            ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-case-5.xml"))
                .withDependency(dependencyToUpgrade)
                .withUseProperties(true));

    Assert.assertTrue("Original POM File is Dirty", context.getPomFile().getDirty());

    String resultPomAsString = new String(context.getPomFile().getResultPomBytes());

    Assert.assertTrue(
        "There must be an unformatted preamble first line",
        resultPomAsString.contains("<project\n"));

    Assert.assertTrue(
        "There must be an unformatted preamble last line",
        resultPomAsString.contains(
            "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">"));

    Assert.assertTrue(
        "There must be a dumb empty element", resultPomAsString.contains("<email></email>"));

    Assert.assertTrue(
        "There must be an empty element with zero spaces", resultPomAsString.contains("<email/>"));
    Assert.assertTrue(
        "There must be an empty element with one space", resultPomAsString.contains("<email />"));
  }

  /**
   * Tests a scenario with an empty element hidden in a comment and validates the resultant POM
   * structure.
   *
   * <p>This test applies modifications related to an empty element hidden within a comment and
   * validates the resulting POM structure.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_add_dependency_to_pom_with_comments_inside() throws Exception {
    Dependency dependencyToUpgrade =
        new Dependency("io.github.pixee", "java-security-toolkit", "1.0.2", null, null, null);

    ProjectModel context =
        performAndAssertModifyPomOperation(
            "case-6",
            ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-case-6.xml"))
                .withDependency(dependencyToUpgrade)
                .withUseProperties(true));

    Assert.assertTrue("Original POM File is Dirty", context.getPomFile().getDirty());

    String resultPomAsString = new String(context.getPomFile().getResultPomBytes());

    Assert.assertTrue(
        "There must be a dumb empty element", resultPomAsString.contains("<email></email>"));
    Assert.assertTrue(
        "There must be an empty element with zero spaces", resultPomAsString.contains("<email/>"));
    Assert.assertTrue(
        "There must be an empty element with one space", resultPomAsString.contains("<email />"));
    Assert.assertTrue(
        "There must be an empty element with three spaces inside a comment",
        resultPomAsString.contains("<email   /> -->"));
  }

  /**
   * Tests a scenario with a property change and validates the resultant POM structure.
   *
   * <p>This test applies modifications related to a property change and validates the resulting POM
   * structure.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_updates_version_in_properties_successfully() throws Exception {
    Dependency dependencyToUpgrade =
        new Dependency("org.dom4j", "dom4j", "1.0.0", null, null, null);

    ProjectModel context =
        performAndAssertModifyPomOperation(
            "case-with-property",
            ProjectModelFactory.load(
                    POMOperatorTest.class.getResource("pom-with-property-simple.xml"))
                .withDependency(dependencyToUpgrade)
                .withUseProperties(true)
                .withSkipIfNewer(true));

    Assert.assertTrue("Original POM File is Dirty", context.getPomFile().getDirty());

    Diff diff =
        getXmlDifferences(
            context.getPomFile().getPomDocument(), context.getPomFile().getResultPom());

    Assert.assertTrue("Document has differences", diff.hasDifferences());

    List<Difference> differenceList = new ArrayList<>();
    for (Difference difference : diff.getDifferences()) {
      differenceList.add(difference);
    }

    Assert.assertEquals("Document has one difference", 1, differenceList.size());

    Assert.assertTrue(
        "Document changes a single version",
        differenceList
            .get(0)
            .toString()
            .startsWith("Expected text value '0.0.1-SNAPSHOT' but was '1.0.0'"));

    Assert.assertEquals(
        "Document changes a property called 'sample.version'",
        differenceList.get(0).getComparison().getTestDetails().getXPath(),
        "/project[1]/properties[1]/sample.version[1]/text()[1]");
  }

  /**
   * Tests a scenario where a property is defined twice, expecting an {@code IllegalStateException}.
   *
   * <p>This test verifies the behavior when a property is defined twice in the POM and expects an
   * {@code IllegalStateException} to be thrown.
   */
  @Test
  void modify_throws_exception_when_dependency_is_defined_twice() {
    Assertions.assertThrows(
        IllegalStateException.class,
        () -> {
          Dependency dependencyToUpgrade =
              new Dependency("org.dom4j", "dom4j", "1.0.0", null, null, null);

          String originalPom =
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                  + "<project\n"
                  + "        xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
                  + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                  + "        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
                  + "    <modelVersion>4.0.0</modelVersion>\n"
                  + "\n"
                  + "    <groupId>br.com.ingenieux</groupId>\n"
                  + "    <artifactId>pom-operator</artifactId>\n"
                  + "    <version>0.0.1-SNAPSHOT</version>\n"
                  + "\n"
                  + "    <properties>\n"
                  + "      <dom4j.version>0.0.1</dom4j.version>\n"
                  + "    </properties>\n"
                  + "\n"
                  + "    <dependencies>\n"
                  + "        <dependency>\n"
                  + "            <groupId>org.dom4j</groupId>\n"
                  + "            <artifactId>dom4j</artifactId>\n"
                  + "            <version>${dom4j.version}</version>\n"
                  + "        </dependency>\n"
                  + "        <dependency>\n"
                  + "            <groupId>org.dom4j</groupId>\n"
                  + "            <artifactId>dom4j-other</artifactId>\n"
                  + "            <version>${dom4j.version}</version>\n"
                  + "        </dependency>\n"
                  + "    </dependencies>\n"
                  + "</project>";

          ProjectModel context =
              ProjectModelFactory.load(
                      new ByteArrayInputStream(originalPom.getBytes(StandardCharsets.UTF_8)))
                  .withDependency(dependencyToUpgrade)
                  .withUseProperties(true)
                  .withOverrideIfAlreadyExists(false)
                  .build();

          POMOperator.modify(context);
        });
  }

  /**
   * Tests a scenario without a property but defining one, expecting differences in the POM.
   *
   * <p>This test verifies the behavior when a property is missing but defined, expecting
   * differences in the POM structure.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_creates_properties_and_adds_version_successfully()
      throws XMLStreamException, URISyntaxException, IOException, DocumentException {
    Dependency dependencyToUpgrade =
        new Dependency("org.dom4j", "dom4j", "1.0.0", null, null, null);

    String originalPom =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<project\n"
            + "        xmlns=\"http://maven.apache.org/POM/4.0.0\"\n"
            + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
            + "        xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
            + "    <modelVersion>4.0.0</modelVersion>\n"
            + "\n"
            + "    <groupId>br.com.ingenieux</groupId>\n"
            + "    <artifactId>pom-operator</artifactId>\n"
            + "    <version>0.0.1-SNAPSHOT</version>\n"
            + "\n"
            + "    <dependencies>\n"
            + "        <dependency>\n"
            + "            <groupId>org.dom4j</groupId>\n"
            + "            <artifactId>dom4j</artifactId>\n"
            + "            <version>0.0.1-SNAPSHOT</version>\n"
            + "        </dependency>\n"
            + "    </dependencies>\n"
            + "</project>";

    ProjectModel context =
        ProjectModelFactory.load(new ByteArrayInputStream(originalPom.getBytes()))
            .withDependency(dependencyToUpgrade)
            .withUseProperties(true)
            .withSkipIfNewer(true)
            .build();

    POMOperator.modify(context);

    Assert.assertTrue(context.getPomFile().getDirty());

    Document originalDocument = context.getPomFile().getPomDocument();
    Document modifiedDocument = context.getPomFile().getResultPom();

    Diff diff = getXmlDifferences(originalDocument, modifiedDocument);

    MatcherAssert.assertThat("Document has differences", diff.hasDifferences());

    Iterable<Difference> differences = diff.getDifferences();
    List<Difference> differenceList = new ArrayList<>();
    for (Difference difference : differences) {
      differenceList.add(difference);
    }

    MatcherAssert.assertThat("Document has several differences", differenceList.size() > 1);
  }

  /**
   * Tests a scenario with a file containing tabs, ensuring proper handling of tab-based formatting.
   *
   * <p>This test verifies the behavior when the POM contains tab-based formatting, ensuring proper
   * handling and expected tab-based structure.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_adds_dependency_to_file_with_tabs()
      throws XMLStreamException, URISyntaxException, IOException, DocumentException {
    Dependency dependencyToUpgrade =
        new Dependency("org.dom4j", "dom4j", "1.0.0", null, null, null);

    String originalPom =
        "<?xml version=\"1.0\"?>\n<project\n\txsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\"\n\txmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n\t<modelVersion>4.0.0</modelVersion>\n\t<parent>\n\t\t<artifactId>build-utils</artifactId>\n\t\t<groupId>org.modafocas.mojo</groupId>\n\t\t<version>0.0.1-SNAPSHOT</version>\n\t\t<relativePath>../pom.xml</relativePath>\n\t</parent>\n\n\t<artifactId>derby-maven-plugin</artifactId>\n\t<packaging>maven-plugin</packaging>\n\n\t<dependencies>\n\t\t<dependency>\n\t\t\t<groupId>org.apache.maven</groupId>\n\t\t\t<artifactId>maven-plugin-api</artifactId>\n\t\t\t<version>2.0</version>\n\t\t</dependency>\n\t\t<dependency>\n\t\t\t<groupId>junit</groupId>\n\t\t\t<artifactId>junit</artifactId>\n\t\t\t<version>3.8.1</version>\n\t\t\t<scope>test</scope>\n\t\t</dependency>\n\t\t<dependency>\n\t\t\t<groupId>org.apache.derby</groupId>\n\t\t\t<artifactId>derby</artifactId>\n\t\t\t<version>${derbyVersion}</version>\n\t\t</dependency>\n\t\t<dependency>\n\t\t\t<groupId>org.apache.derby</groupId>\n\t\t\t<artifactId>derbynet</artifactId>\n\t\t\t<version>${derbyVersion}</version>\n\t\t</dependency>\n\t\t<dependency>\n\t\t\t<groupId>org.apache.derby</groupId>\n\t\t\t<artifactId>derbyclient</artifactId>\n\t\t\t<version>${derbyVersion}</version>\n\t\t</dependency>\n\t\t<dependency>\n\t\t\t<groupId>commons-io</groupId>\n\t\t\t<artifactId>commons-io</artifactId>\n\t\t\t<version>1.4</version>\n\t\t\t<type>jar</type>\n\t\t\t<scope>compile</scope>\n\t\t</dependency>\n\t</dependencies>\n\n\t<properties>\n\t\t<derbyVersion>10.6.2.1</derbyVersion>\n\t</properties>\n</project>\n";

    ProjectModel context =
        ProjectModelFactory.load(
                new ByteArrayInputStream(originalPom.getBytes(Charset.defaultCharset())))
            .withDependency(dependencyToUpgrade)
            .withUseProperties(true)
            .withSkipIfNewer(true)
            .build();

    POMOperator.modify(context);

    Assert.assertTrue(context.getPomFile().getDirty());

    String resultPom =
        new String(context.getPomFile().getResultPomBytes(), Charset.defaultCharset());

    // aldrin: uncomment this to check out formatting - useful for the next section
    // System.out.println(resultPom);

    Assert.assertTrue(
        "Document should have a tab-based string",
        resultPom.contains(
            "\n\t\t<dependency>\n\t\t\t<groupId>org.dom4j</groupId>\n\t\t\t<artifactId>dom4j</artifactId>\n\t\t</dependency>\n"));
  }

  /**
   * Tests a scenario with an empty element from a customer's POM file and validates the resultant
   * POM.
   *
   * <p>This test applies modifications related to an empty element from a customer's POM and
   * validates the resulting POM structure.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void modify_adds_dependency_to_pom_with_empty_elements_from_customer() throws Exception {
    Dependency dependencyToUpgrade =
        new Dependency("io.github.pixee", "java-security-toolkit", "1.0.2", null, null, null);

    ProjectModel context =
        performAndAssertModifyPomOperation(
            "hack23-cia",
            ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-hack23-cia.xml"))
                .withDependency(dependencyToUpgrade)
                .withUseProperties(true));

    Assert.assertTrue(context.getPomFile().getDirty());

    String resultPomAsString = new String(context.getPomFile().getResultPomBytes());

    Assert.assertTrue(
        "There must be an untouched empty element",
        resultPomAsString.contains("<sonar.zaproxy.reportPath></sonar.zaproxy.reportPath>"));

    Assert.assertTrue(
        "There must be an untouched element with attributes",
        resultPomAsString.contains(
            "<mkdir dir=\"${project.build.directory}/dependency\"></mkdir>"));

    Assert.assertTrue(
        "The <DependencyConvergence> tag must be untouched by test.",
        resultPomAsString.contains(
            "                <DependencyConvergence></DependencyConvergence>"));
  }

  /**
   * Tests the scenario where an insert operation should fail due to an existing dependency.
   *
   * <p>This test verifies the behavior when attempting to insert a dependency that already exists,
   * expecting a failure scenario.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void insert_should_fail_because_dependency_exists() throws Exception {
    Dependency dependencyToUpgrade =
        new Dependency("org.dom4j", "dom4j", "2.0.2", null, null, null);

    ProjectModel context =
        performAndAssertInsertPomOperation(
            "pom-case-3-insert-fails",
            ProjectModelFactory.load(POMOperatorTest.class.getResource("pom-case-3.xml"))
                .withDependency(dependencyToUpgrade)
                .withSkipIfNewer(true));

    Assert.assertFalse("Original POM File is Dirty", context.getPomFile().getDirty());

    List<Dependency> resolvedDeps =
        POMOperator.queryDependency(
                ProjectModelFactory.load(
                        POMOperatorTest.class.getResource("pom-pom-case-3-insert-fails-result.xml"))
                    .withSafeQueryType()
                    .build())
            .stream()
            .toList();

    final List<Dependency> dom4jDependency =
        resolvedDeps.stream()
            .filter(dependency -> dependency.matchesWithoutConsideringVersion(dependencyToUpgrade))
            .toList();

    Assert.assertTrue("only one dom4j dependency exists", dom4jDependency.size() == 1);
    Assert.assertTrue(
        "dependency doesn't match because of version",
        !dom4jDependency.get(0).equals(dependencyToUpgrade));
  }

  /**
   * Tests the scenario where an insert operation performs successfully.
   *
   * <p>This test verifies the successful insertion of a dependency into the POM.
   *
   * @throws Exception if an error occurs during the test.
   */
  @Test
  void insert_should_perform_gracefully() throws Exception {
    Dependency dependencyToUpgrade =
        new Dependency("org.dom4j", "dom4j", "1.0.0", null, null, null);

    ProjectModel context =
        performAndAssertInsertPomOperation(
            "inserts-dependency",
            ProjectModelFactory.load(
                    POMOperatorTest.class.getResource("pom-without-dependencies.xml"))
                .withDependency(dependencyToUpgrade)
                .withUseProperties(true)
                .withSkipIfNewer(true));

    Assert.assertTrue("Original POM File is Dirty", context.getPomFile().getDirty());

    List<Dependency> resolvedDeps =
        POMOperator.queryDependency(
                ProjectModelFactory.load(
                        POMOperatorTest.class.getResource("pom-inserts-dependency-result.xml"))
                    .withSafeQueryType()
                    .build())
            .stream()
            .toList();

    Assert.assertTrue("Must have one dependencies", 1 == resolvedDeps.size());
    Assert.assertTrue(
        "dom4j is the desired dependency", resolvedDeps.get(0).equals(dependencyToUpgrade));
  }
}
