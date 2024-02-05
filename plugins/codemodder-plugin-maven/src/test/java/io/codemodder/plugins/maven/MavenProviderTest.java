package io.codemodder.plugins.maven;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.codemodder.*;
import io.codemodder.codetf.CodeTFChange;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.codetf.CodeTFDiffSide;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Unit tests for {@link MavenProvider}. We bend over backwards here not to test the pom-operator
 * types.
 */
final class MavenProviderTest {

  private static class TestPomModifier implements MavenProvider.PomModifier {
    @Override
    public void modify(final Path path, byte[] contents) {}
  }

  private TestPomModifier pomModifier;
  private DependencyDescriptor defaultDescriptor;
  private PomFileUpdater pomFileUpdater;
  private PomFileFinder pomFileFinder;
  private Path projectDir;
  private DependencyGAV marsDependency1;
  private DependencyGAV marsDependency2;
  private DependencyGAV venusDependency;
  private DependencyGAV jspDependency;

  private static Path module1Pom;
  private static Path module2Pom;
  private static Path module3Pom;
  private static Path rootPom;
  private static Path marsJavaFile;
  private static Path venusJavaFile;
  private static Path cloud9JavaFile;
  private static Path irrelevantFile;
  private static Path jspFile;
  private static Path gerrardJavaFile;

  /**
   * Set up the mocks to test analyzing this simulated project with these files:
   *
   * <ul>
   *   <li>/project/module1/src/main/java/com/acme/Mars.java
   *   <li>/project/module1/src/main/java/com/acme/Venus.java
   *   <li>/project/module1/pom.xml
   *   <li>/project/module2/src/main/java/com/acme/Cloud9.java
   *   <li>/project/module2/pom.xml
   *   <li>/project/module3/src/main/java/com/acme/Gerrard.java
   *   <li>/project/module3/pom.xml
   *   <li>/project/src/main/resources/webapp/WEB-INF/page.jsp
   *   <li>/project/pom.xml
   *   <li>/project/irrelevant
   * </ul>
   */
  @BeforeEach
  void setup(final @TempDir Path projectDir) throws IOException {

    this.projectDir = projectDir;
    this.defaultDescriptor = DependencyDescriptor.createMarkdownDescriptor();

    marsJavaFile = this.projectDir.resolve("module1/src/main/java/com/acme/Mars.java");
    venusJavaFile = this.projectDir.resolve("module1/src/main/java/com/acme/Venus.java");
    module1Pom = this.projectDir.resolve("module1/pom.xml");

    cloud9JavaFile = this.projectDir.resolve("module2/src/main/java/com/acme/Cloud9.java");
    module2Pom = this.projectDir.resolve("module2/pom.xml");

    gerrardJavaFile = this.projectDir.resolve("module3/src/main/java/com/acme/Gerrard.java");
    module3Pom = this.projectDir.resolve("module3/pom.xml");

    jspFile = this.projectDir.resolve("src/main/resources/webapp/WEB-INF/page.jsp");
    rootPom = this.projectDir.resolve("pom.xml");

    irrelevantFile = this.projectDir.resolve("irrelevant");
    Set<Path> files =
        Set.of(
            irrelevantFile,
            marsJavaFile,
            venusJavaFile,
            cloud9JavaFile,
            module1Pom,
            module2Pom,
            gerrardJavaFile,
            module3Pom,
            rootPom,
            jspFile);
    files.forEach(
        file -> {
          try {
            Files.createDirectories(file.getParent());
            Files.createFile(file);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });

    // everybody gets a dependency except the file from module3
    marsDependency1 = DependencyGAV.createDefault("com.acme.mars", "mars1", "1.0.1");
    marsDependency2 = DependencyGAV.createDefault("com.acme.mars", "mars2", "1.0.1");
    venusDependency = DependencyGAV.createDefault("com.acme.venus", "venus", "1.0.2");
    jspDependency = DependencyGAV.createDefault("com.acme.jsp", "jsp", "1.0.4");

    this.pomFileFinder = mock(PomFileFinder.class);
    when(pomFileFinder.findForFile(any(), eq(marsJavaFile))).thenReturn(Optional.of(module1Pom));
    when(pomFileFinder.findForFile(any(), eq(venusJavaFile))).thenReturn(Optional.of(module2Pom));
    when(pomFileFinder.findForFile(any(), eq(gerrardJavaFile))).thenReturn(Optional.of(module3Pom));
    when(pomFileFinder.findForFile(any(), eq(jspFile))).thenReturn(Optional.of(rootPom));

    this.pomFileUpdater = mock(PomFileUpdater.class);

    this.pomModifier = new TestPomModifier();
  }

  @Test
  void it_returns_changeset_when_no_issues() throws IOException {
    Files.writeString(module1Pom, simplePom);

    MavenProvider provider =
        new MavenProvider(
            new MavenProvider.DefaultPomModifier(),
            new MavenProvider.DefaultPomFileFinder(),
            defaultDescriptor);

    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir, marsJavaFile, List.of(marsDependency1, marsDependency2));

    // no errors, success!
    assertThat(result.erroredFiles()).isEmpty();

    // we've updated all the poms, so we merge this with the pre-existing one change
    List<CodeTFChangesetEntry> changes = result.packageChanges();
    assertThat(changes.size()).isEqualTo(2);

    // we injected all the dependencies, total success!
    assertThat(result.injectedPackages()).containsOnly(marsDependency1, marsDependency2);

    // we don't have license facts for these dependencies, so we should be silent on their license!
    boolean matchedLicenseFacts =
        changes.stream()
            .map(CodeTFChangesetEntry::getChanges)
            .flatMap(Collection::stream)
            .anyMatch(c -> c.getDescription().contains("License: "));
    assertThat(matchedLicenseFacts).isFalse();
  }

  private static Stream<Arguments> expectedChangeDescriptionLocations() {
    return Stream.of(
        Arguments.of(simplePom, 7), // injects all new text in one delta
        Arguments.of(
            simplePomWithExistingSections,
            16), // already has both sections, record at dependencies section
        Arguments.of(
            webgoatPomThatJustNeedsUpgrades,
            151), // just updating the version number here in the properties
        Arguments.of(webgoatPom, 413) // add to the end of the dependencies section
        );
  }

  /**
   * When we inject into a pom, we inject into the properties section and the dependencies section.
   */
  @ParameterizedTest
  @MethodSource("expectedChangeDescriptionLocations")
  void it_places_library_facts_in_correct_pom_place(
      final String pomContents, final int libraryFactsLineTarget) throws IOException {
    Files.writeString(module1Pom, pomContents);
    MavenProvider provider =
        new MavenProvider(
            new MavenProvider.DefaultPomModifier(),
            new MavenProvider.DefaultPomFileFinder(),
            defaultDescriptor);

    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir, marsJavaFile, List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT));

    // shouldn't be any failures -- straightforward change
    assertThat(result.skippedPackages()).isEmpty();
    assertThat(result.erroredFiles()).isEmpty();

    // we only injected the toolkit -- verify that
    List<DependencyGAV> injectedPackages = result.injectedPackages();
    assertThat(injectedPackages.size()).isEqualTo(1);
    DependencyGAV injectedPackage = injectedPackages.get(0);
    assertThat(injectedPackage).isEqualTo(DependencyGAV.JAVA_SECURITY_TOOLKIT);

    List<CodeTFChangesetEntry> changesets = result.packageChanges();
    assertThat(changesets.size()).isEqualTo(1);
    CodeTFChangesetEntry change = changesets.get(0);
    List<CodeTFChange> changes = change.getChanges();
    assertThat(changes.size()).isEqualTo(1);
    CodeTFChange lineChange = changes.get(0);
    assertThat(lineChange.getDescription()).contains("License: ");
    assertThat(lineChange.getLineNumber()).isEqualTo(libraryFactsLineTarget);
  }

  /**
   * Sometimes we'll inject the GAV into the parent's dependencyManagement section, and the child
   * will just get the group and artifact. Confirm we are intentional and not redundant where we
   * place those facts.
   */
  @Test
  void it_returns_expected_changeset_when_using_parent_pom() throws IOException {
    Files.writeString(module1Pom, childPomWithJustDependencies);
    Files.writeString(rootPom, parentPomWithDependencyMgmtContents);

    MavenProvider provider =
        new MavenProvider(
            new MavenProvider.DefaultPomModifier(),
            new MavenProvider.DefaultPomFileFinder(),
            defaultDescriptor);

    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir, marsJavaFile, List.of(marsDependency1, marsDependency2));

    // no errors, success!
    assertThat(result.erroredFiles()).isEmpty();

    // we've updated all the poms, so we merge this with the pre-existing one change
    List<CodeTFChangesetEntry> changes = result.packageChanges();
    assertThat(changes.size()).isEqualTo(4);

    // module1/pom.xml adding mars1 to dependencies
    assertThat(changes.get(0).getPath()).isEqualTo("module1/pom.xml");
    assertThat(changes.get(0).getChanges().get(0).getLineNumber()).isEqualTo(19);
    assertThat(changes.get(0).getChanges().get(0).getDiffSide()).isEqualTo(CodeTFDiffSide.RIGHT);

    // pom.xml adding mars1 to dependency management
    assertThat(changes.get(1).getPath()).isEqualTo("pom.xml");
    assertThat(changes.get(1).getChanges().get(0).getLineNumber()).isEqualTo(15);
    assertThat(changes.get(1).getChanges().get(0).getDiffSide()).isEqualTo(CodeTFDiffSide.RIGHT);

    // module1/pom.xml adding mars2 to dependencies
    assertThat(changes.get(2).getPath()).isEqualTo("module1/pom.xml");
    assertThat(changes.get(2).getChanges().get(0).getLineNumber()).isEqualTo(23);
    assertThat(changes.get(2).getChanges().get(0).getDiffSide()).isEqualTo(CodeTFDiffSide.RIGHT);

    // pom.xml adding mars2 to dependency management
    assertThat(changes.get(3).getPath()).isEqualTo("pom.xml");
    assertThat(changes.get(3).getChanges().get(0).getLineNumber()).isEqualTo(20);
    assertThat(changes.get(3).getChanges().get(0).getDiffSide()).isEqualTo(CodeTFDiffSide.RIGHT);

    // we don't have license facts for these dependencies, so we should be silent on their license!
    boolean matchedLicenseFacts =
        changes.stream()
            .map(CodeTFChangesetEntry::getChanges)
            .flatMap(Collection::stream)
            .anyMatch(c -> c.getDescription().contains("License: "));
    assertThat(matchedLicenseFacts).isFalse();

    // we injected all the dependencies, total success!
    assertThat(Files.readString(rootPom))
        .isEqualToIgnoringWhitespace(parentPomWithDependencyMgmtContentsAfter);
    assertThat(Files.readString(module1Pom))
        .isEqualToIgnoringWhitespace(childPomWithJustDependenciesAfter);
    assertThat(result.injectedPackages()).containsOnly(marsDependency1, marsDependency2);
  }

  @Test
  void it_captures_deeper_dependency_facts_when_available() throws IOException {
    Files.writeString(module1Pom, simplePom);

    MavenProvider provider =
        new MavenProvider(
            new MavenProvider.DefaultPomModifier(),
            new MavenProvider.DefaultPomFileFinder(),
            defaultDescriptor);

    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir,
            marsJavaFile,
            List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT, DependencyGAV.OWASP_XSS_JAVA_ENCODER));

    // we injected all the dependencies, total success!
    assertThat(result.injectedPackages())
        .containsOnly(DependencyGAV.JAVA_SECURITY_TOOLKIT, DependencyGAV.OWASP_XSS_JAVA_ENCODER);

    List<CodeTFChangesetEntry> changes = result.packageChanges();

    boolean matchedXssJavaEncoderFacts =
        changes.stream()
            .map(CodeTFChangesetEntry::getChanges)
            .flatMap(Collection::stream)
            .anyMatch(c -> c.getDescription().contains("License: BSD 3-Clause"));
    assertThat(matchedXssJavaEncoderFacts).isTrue();

    boolean matchedToolkitFacts =
        changes.stream()
            .map(CodeTFChangesetEntry::getChanges)
            .flatMap(Collection::stream)
            .filter(c -> "true".equals(c.getProperties().get("contextual_description")))
            .anyMatch(c -> c.getDescription().contains("License: MIT"));
    assertThat(matchedToolkitFacts).isTrue();
  }

  @Test
  void it_returns_empty_when_no_pom() throws IOException {
    when(pomFileFinder.findForFile(any(), any())).thenReturn(Optional.empty());

    MavenProvider provider = new MavenProvider(pomModifier, pomFileFinder, defaultDescriptor);

    DependencyUpdateResult result =
        provider.updateDependencies(projectDir, marsJavaFile, List.of(marsDependency1));

    assertThat(result.skippedPackages()).isEmpty();
    assertThat(result.packageChanges()).isEmpty();
    assertThat(result.erroredFiles()).isEmpty();
    assertThat(result.injectedPackages()).isEmpty();
  }

  @Test
  void it_handles_when_already_present() throws IOException {

    String pom;
    pom = "<project>";
    pom += "  <modelVersion>4.0.0</modelVersion>";
    pom += "  <groupId>com.acme</groupId>";
    pom += "  <artifactId>mavenproject1</artifactId>";
    pom += "  <version>1.0-SNAPSHOT</version>";
    pom += "  <packaging>jar</packaging>";
    pom += "  <dependencies>";
    pom += "    <dependency>";
    pom += "      <groupId>org.apache</groupId>";
    pom += "      <artifactId>kafka</artifactId>";
    pom += "      <version>2.0</version>";
    pom += "    </dependency>";
    pom += "  </dependencies>";
    pom += "</project>";

    Files.writeString(module1Pom, pom);
    when(pomFileFinder.findForFile(any(), any())).thenReturn(Optional.of(module1Pom));

    // this module is already present
    DependencyGAV alreadyPresent = DependencyGAV.createDefault("org.apache", "kafka", "2.0");

    MavenProvider provider = new MavenProvider(pomModifier);
    DependencyUpdateResult result =
        provider.updateDependencies(projectDir, marsJavaFile, List.of(alreadyPresent));

    List<DependencyGAV> skipped = result.skippedPackages();
    assertThat(skipped).containsOnly(alreadyPresent);
    assertThat(result.packageChanges()).isEmpty();
    assertThat(result.erroredFiles()).isEmpty();
    assertThat(result.injectedPackages()).isEmpty();
  }

  @Test
  void it_handles_pom_update_failure() throws IOException {
    MavenProvider.PomModifier pomModifier = mock(MavenProvider.PomModifier.class);

    Files.writeString(module1Pom, simplePom);

    // introduce a failure
    doThrow(new IOException("failed to update pom")).when(pomModifier).modify(any(), any());

    MavenProvider provider = new MavenProvider(pomModifier);

    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir, marsJavaFile, List.of(venusDependency, jspDependency));

    // the failure we expected should be there
    assertThat(result.skippedPackages()).isEmpty();
    assertThat(result.erroredFiles()).containsOnly(module1Pom);
    assertThat(result.injectedPackages()).isEmpty();
    assertThat(result.packageChanges()).isEmpty();
  }

  @Test
  void it_updates_poms_correctly() throws IOException {
    Files.writeString(module1Pom, simplePom);

    MavenProvider provider = new MavenProvider();

    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir, module1Pom, List.of(marsDependency1, marsDependency2, venusDependency));

    assertThat(result.packageChanges().size()).isEqualTo(3);

    CodeTFChangesetEntry changesetEntry = result.packageChanges().iterator().next();
    assertThat(changesetEntry.getPath()).isEqualTo("module1/pom.xml");
    String updatedPomContents =
        Files.readString(projectDir.resolve(changesetEntry.getPath()), StandardCharsets.UTF_8);
    assertThat(updatedPomContents).isEqualToIgnoringWhitespace(simplePomAfterChanges);
  }

  @Test
  void it_finds_correct_poms() throws IOException {
    PomFileFinder pomFinder = new MavenProvider.DefaultPomFileFinder();
    for (Pair<Path, Optional<Path>> pair :
        Arrays.asList(
            Pair.of(marsJavaFile, Optional.of(module1Pom)),
            Pair.of(venusJavaFile, Optional.of(module1Pom)),
            Pair.of(cloud9JavaFile, Optional.of(module2Pom)),
            Pair.of(gerrardJavaFile, Optional.of(module3Pom)),
            Pair.of(jspFile, Optional.of(rootPom)),
            Pair.of(irrelevantFile, Optional.of(rootPom)))) {
      Optional<Path> pom = pomFinder.findForFile(this.projectDir, pair.getLeft());
      assertThat(pom.isPresent()).isTrue();
      assertThat(pom.get()).isEqualTo(pair.getRight().get());
    }
  }

  private static final String simplePomWithExistingSections =
      """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>link.sharpe</groupId>
                      <artifactId>mavenproject1-child</artifactId>
                      <version>1.0-SNAPSHOT</version>
                      <properties>
                        <my>property</my>
                        <bar>foo</bar>
                        <foo>bar</foo>
                        <other>value</other>
                        <last>one</last>
                      </properties>

                      <dependencies>

                      </dependencies>
                  </project>""";

  private static final String simplePom =
      """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                      <modelVersion>4.0.0</modelVersion>
                      <groupId>link.sharpe</groupId>
                      <artifactId>mavenproject1-child</artifactId>
                      <version>1.0-SNAPSHOT</version>
                  </project>""";

  private static final String simplePomAfterChanges =
      """
                  <?xml version="1.0" encoding="UTF-8"?>
                    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                        <modelVersion>4.0.0</modelVersion>
                        <groupId>link.sharpe</groupId>
                        <artifactId>mavenproject1-child</artifactId>
                        <version>1.0-SNAPSHOT</version>
                        <dependencyManagement>
                            <dependencies>
                                <dependency>
                                    <groupId>com.acme.mars</groupId>
                                    <artifactId>mars1</artifactId>
                                    <version>${versions.mars1}</version>
                                </dependency>
                                <dependency>
                                    <groupId>com.acme.mars</groupId>
                                    <artifactId>mars2</artifactId>
                                    <version>${versions.mars2}</version>
                                </dependency>
                                <dependency>
                                    <groupId>com.acme.venus</groupId>
                                    <artifactId>venus</artifactId>
                                    <version>${versions.venus}</version>
                                </dependency>
                            </dependencies>
                        </dependencyManagement>
                        <properties>
                            <versions.mars1>1.0.1</versions.mars1>
                            <versions.mars2>1.0.1</versions.mars2>
                            <versions.venus>1.0.2</versions.venus>
                        </properties>
                        <dependencies>
                            <dependency>
                                <groupId>com.acme.mars</groupId>
                                <artifactId>mars1</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>com.acme.mars</groupId>
                                <artifactId>mars2</artifactId>
                            </dependency>
                            <dependency>
                                <groupId>com.acme.venus</groupId>
                                <artifactId>venus</artifactId>
                            </dependency>
                        </dependencies>
                    </project>""";

  private static final String parentPomWithDependencyMgmtContents =
      """
          <?xml version="1.0" encoding="UTF-8"?>
          <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>link.sharpe</groupId>
              <artifactId>mavenproject1-parent</artifactId>
              <version>1.0-SNAPSHOT</version>
              <packaging>pom</packaging>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.other</groupId>
                    <artifactId>other</artifactId>
                    <version>1.0.0</version>
                  </dependency>
                </dependencies>
              </dependencyManagement>
          </project>
          """;

  private static final String parentPomWithDependencyMgmtContentsAfter =
      """
          <?xml version="1.0" encoding="UTF-8"?>
          <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
              <modelVersion>4.0.0</modelVersion>
              <groupId>link.sharpe</groupId>
              <artifactId>mavenproject1-parent</artifactId>
              <version>1.0-SNAPSHOT</version>
              <packaging>pom</packaging>
              <dependencyManagement>
                <dependencies>
                  <dependency>
                    <groupId>com.other</groupId>
                    <artifactId>other</artifactId>
                    <version>1.0.0</version>
                  </dependency>
                      <dependency>
                        <groupId>com.acme.mars</groupId>
                        <artifactId>mars1</artifactId>
                        <version>${versions.mars1}</version>
                      </dependency>
                      <dependency>
                        <groupId>com.acme.mars</groupId>
                        <artifactId>mars2</artifactId>
                        <version>${versions.mars2}</version>
                      </dependency>
                </dependencies>
              </dependencyManagement>
              <properties>
                  <versions.mars1>1.0.1</versions.mars1>
                  <versions.mars2>1.0.1</versions.mars2>
              </properties>
          </project>
          """;

  private static final String childPomWithJustDependencies =
      """
              <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>link.sharpe</groupId>
                      <artifactId>mavenproject1-parent</artifactId>
                      <version>1.0-SNAPSHOT</version>
                      <relativePath>../pom.xml</relativePath>
                    </parent>
                    <groupId>link.sharpe</groupId>
                    <artifactId>mavenproject1-child</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.acme.venus</groupId>
                            <artifactId>venus</artifactId>
                            <version>1.0.2</version>
                        </dependency>
                    </dependencies>
                </project>
                """;

  private static final String childPomWithJustDependenciesAfter =
      """
              <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <parent>
                      <groupId>link.sharpe</groupId>
                      <artifactId>mavenproject1-parent</artifactId>
                      <version>1.0-SNAPSHOT</version>
                      <relativePath>../pom.xml</relativePath>
                    </parent>
                    <groupId>link.sharpe</groupId>
                    <artifactId>mavenproject1-child</artifactId>
                    <version>1.0-SNAPSHOT</version>
                    <dependencies>
                        <dependency>
                            <groupId>com.acme.venus</groupId>
                            <artifactId>venus</artifactId>
                            <version>1.0.2</version>
                        </dependency>
                        <dependency>
                            <groupId>com.acme.mars</groupId>
                            <artifactId>mars1</artifactId>
                        </dependency>
                        <dependency>
                            <groupId>com.acme.mars</groupId>
                            <artifactId>mars2</artifactId>
                        </dependency>
                    </dependencies>
                </project>
                """;

  private static final String webgoatPom =
      """
          <?xml version="1.0" encoding="UTF-8"?>
          <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
            <modelVersion>4.0.0</modelVersion>

            <parent>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-parent</artifactId>
              <version>3.1.0</version>
            </parent>

            <groupId>org.owasp.webgoat</groupId>
            <artifactId>webgoat</artifactId>
            <version>2023.5-SNAPSHOT</version>
            <packaging>jar</packaging>

            <name>WebGoat</name>
            <description>WebGoat, a deliberately insecure Web Application</description>
            <url>https://github.com/WebGoat/WebGoat</url>
            <inceptionYear>2006</inceptionYear>
            <organization>
              <name>OWASP</name>
              <url>https://github.com/WebGoat/WebGoat/</url>
            </organization>
            <licenses>
              <license>
                <name>GNU General Public License, version 2</name>
                <url>https://www.gnu.org/licenses/gpl-2.0.txt</url>
              </license>
            </licenses>

            <developers>
              <developer>
                <id>mayhew64</id>
                <name>Bruce Mayhew</name>
                <email>webgoat@owasp.org</email>
                <organization>OWASP</organization>
                <organizationUrl>https://github.com/WebGoat/WebGoat</organizationUrl>
              </developer>
              <developer>
                <id>nbaars</id>
                <name>Nanne Baars</name>
                <email>nanne.baars@owasp.org</email>
                <organizationUrl>https://github.com/nbaars</organizationUrl>
                <timezone>Europe/Amsterdam</timezone>
              </developer>
              <developer>
                <id>misfir3</id>
                <name>Jason White</name>
                <email>jason.white@owasp.org</email>
              </developer>
              <developer>
                <id>zubcevic</id>
                <name>René Zubcevic</name>
                <email>rene.zubcevic@owasp.org</email>
              </developer>
              <developer>
                <id>aolle</id>
                <name>Àngel Ollé Blázquez</name>
                <email>angel@olleb.com</email>
              </developer>
              <developer>
                <id>jwayman</id>
                <name>Jeff Wayman</name>
                <email></email>
              </developer>
              <developer>
                <id>dcowden</id>
                <name>Dave Cowden</name>
                <email></email>
              </developer>
              <developer>
                <id>lawson89</id>
                <name>Richard Lawson</name>
                <email></email>
              </developer>
              <developer>
                <id>dougmorato</id>
                <name>Doug Morato</name>
                <email>doug.morato@owasp.org</email>
                <organization>OWASP</organization>
                <organizationUrl>https://github.com/dougmorato</organizationUrl>
                <timezone>America/New_York</timezone>
                <properties>
                  <picUrl>https://avatars2.githubusercontent.com/u/9654?v=3&amp;s=150</picUrl>
                </properties>
              </developer>
            </developers>

            <mailingLists>
              <mailingList>
                <name>OWASP WebGoat Mailing List</name>
                <subscribe>https://lists.owasp.org/mailman/listinfo/owasp-webgoat</subscribe>
                <unsubscribe>Owasp-webgoat-request@lists.owasp.org</unsubscribe>
                <post>owasp-webgoat@lists.owasp.org</post>
                <archive>http://lists.owasp.org/pipermail/owasp-webgoat/</archive>
              </mailingList>
            </mailingLists>
            <scm>
              <connection>scm:git:git@github.com:WebGoat/WebGoat.git</connection>
              <developerConnection>scm:git:git@github.com:WebGoat/WebGoat.git</developerConnection>
              <tag>HEAD</tag>
              <url>https://github.com/WebGoat/WebGoat</url>
            </scm>

            <issueManagement>
              <system>Github Issues</system>
              <url>https://github.com/WebGoat/WebGoat/issues</url>
            </issueManagement>

            <properties>
              <!-- Shared properties with plugins and version numbers across submodules-->
              <asciidoctorj.version>2.5.10</asciidoctorj.version>
              <!-- Upgrading needs UI work in WebWolf -->
              <bootstrap.version>3.3.7</bootstrap.version>
              <cglib.version>3.3.0</cglib.version>
              <!-- do not update necessary for lesson -->
              <checkstyle.version>3.3.0</checkstyle.version>
              <commons-collections.version>3.2.1</commons-collections.version>
              <commons-io.version>2.11.0</commons-io.version>
              <commons-lang3.version>3.12.0</commons-lang3.version>
              <commons-text.version>1.10.0</commons-text.version>
              <guava.version>32.1.1-jre</guava.version>
              <jacoco.version>0.8.10</jacoco.version>
              <java.version>17</java.version>
              <jaxb.version>2.3.1</jaxb.version>
              <jjwt.version>0.9.1</jjwt.version>
              <jose4j.version>0.9.3</jose4j.version>
              <jquery.version>3.6.4</jquery.version>
              <jsoup.version>1.16.1</jsoup.version>
              <maven-compiler-plugin.version>3.8.0</maven-compiler-plugin.version>
              <maven-failsafe-plugin.version>2.22.0</maven-failsafe-plugin.version>
              <maven-jar-plugin.version>3.1.2</maven-jar-plugin.version>
              <maven-javadoc-plugin.version>3.1.1</maven-javadoc-plugin.version>
              <maven-source-plugin.version>3.1.0</maven-source-plugin.version>
              <maven-surefire-plugin.version>3.1.2</maven-surefire-plugin.version>
              <maven.compiler.source>17</maven.compiler.source>
              <maven.compiler.target>17</maven.compiler.target>
              <pmd.version>3.15.0</pmd.version>
              <!-- Use UTF-8 Encoding -->
              <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
              <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
              <thymeleaf.version>3.1.1.RELEASE</thymeleaf.version>
              <webdriver.version>5.3.2</webdriver.version>
              <webgoat.port>8080</webgoat.port>
              <webwolf.port>9090</webwolf.port>
              <wiremock.version>2.27.2</wiremock.version>
              <xml-resolver.version>1.2</xml-resolver.version>
              <xstream.version>1.4.5</xstream.version>
              <!-- do not update necessary for lesson -->
              <zxcvbn.version>1.8.0</zxcvbn.version>
            </properties>

            <dependencyManagement>
              <dependencies>

                <dependency>
                  <groupId>org.ow2.asm</groupId>
                  <artifactId>asm</artifactId>
                  <version>9.5</version>
                </dependency>

                <dependency>
                  <groupId>org.apache.commons</groupId>
                  <artifactId>commons-exec</artifactId>
                  <version>1.3</version>
                </dependency>
                <dependency>
                  <groupId>org.asciidoctor</groupId>
                  <artifactId>asciidoctorj</artifactId>
                  <version>${asciidoctorj.version}</version>
                </dependency>
                <dependency>
                  <!-- jsoup HTML parser library @ https://jsoup.org/ -->
                  <groupId>org.jsoup</groupId>
                  <artifactId>jsoup</artifactId>
                  <version>${jsoup.version}</version>
                </dependency>
                <dependency>
                  <groupId>com.nulab-inc</groupId>
                  <artifactId>zxcvbn</artifactId>
                  <version>${zxcvbn.version}</version>
                </dependency>
                <dependency>
                  <groupId>com.thoughtworks.xstream</groupId>
                  <artifactId>xstream</artifactId>
                  <version>${xstream.version}</version>
                </dependency>
                <dependency>
                  <groupId>cglib</groupId>
                  <artifactId>cglib-nodep</artifactId>
                  <version>${cglib.version}</version>
                </dependency>
                <dependency>
                  <groupId>xml-resolver</groupId>
                  <artifactId>xml-resolver</artifactId>
                  <version>${xml-resolver.version}</version>
                </dependency>
                <dependency>
                  <groupId>io.jsonwebtoken</groupId>
                  <artifactId>jjwt</artifactId>
                  <version>${jjwt.version}</version>
                </dependency>
                <dependency>
                  <groupId>com.google.guava</groupId>
                  <artifactId>guava</artifactId>
                  <version>${guava.version}</version>
                </dependency>
                <dependency>
                  <groupId>commons-io</groupId>
                  <artifactId>commons-io</artifactId>
                  <version>${commons-io.version}</version>
                </dependency>
                <dependency>
                  <groupId>org.apache.commons</groupId>
                  <artifactId>commons-text</artifactId>
                  <version>${commons-text.version}</version>
                </dependency>
                <dependency>
                  <groupId>org.bitbucket.b_c</groupId>
                  <artifactId>jose4j</artifactId>
                  <version>${jose4j.version}</version>
                </dependency>
                <dependency>
                  <groupId>org.webjars</groupId>
                  <artifactId>bootstrap</artifactId>
                  <version>${bootstrap.version}</version>
                </dependency>
                <dependency>
                  <groupId>org.webjars</groupId>
                  <artifactId>jquery</artifactId>
                  <version>${jquery.version}</version>
                </dependency>
                <dependency>
                  <groupId>com.github.tomakehurst</groupId>
                  <artifactId>wiremock</artifactId>
                  <version>${wiremock.version}</version>
                </dependency>
                <dependency>
                  <groupId>io.github.bonigarcia</groupId>
                  <artifactId>webdrivermanager</artifactId>
                  <version>${webdriver.version}</version>
                </dependency>
                <dependency>
                  <groupId>org.apache.commons</groupId>
                  <artifactId>commons-compress</artifactId>
                  <version>1.23.0</version>
                </dependency>
                <dependency>
                  <groupId>org.jruby</groupId>
                  <artifactId>jruby</artifactId>
                  <version>9.4.2.0</version>
                </dependency>
              </dependencies>
            </dependencyManagement>
            <dependencies>
              <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-exec</artifactId>
              </dependency>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-validation</artifactId>
              </dependency>
              <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <scope>provided</scope>
                <optional>true</optional>
              </dependency>
              <dependency>
                <groupId>javax.xml.bind</groupId>
                <artifactId>jaxb-api</artifactId>
                <version>${jaxb.version}</version>
              </dependency>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-undertow</artifactId>
              </dependency>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
                <exclusions>
                  <exclusion>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-tomcat</artifactId>
                  </exclusion>
                </exclusions>
              </dependency>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-actuator</artifactId>
              </dependency>
              <dependency>
                <groupId>org.flywaydb</groupId>
                <artifactId>flyway-core</artifactId>
              </dependency>
              <dependency>
                <groupId>org.asciidoctor</groupId>
                <artifactId>asciidoctorj</artifactId>
              </dependency>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-jpa</artifactId>
              </dependency>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-security</artifactId>
              </dependency>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-thymeleaf</artifactId>
              </dependency>
              <dependency>
                <groupId>org.thymeleaf.extras</groupId>
                <artifactId>thymeleaf-extras-springsecurity6</artifactId>
              </dependency>
              <dependency>
                <groupId>jakarta.servlet</groupId>
                <artifactId>jakarta.servlet-api</artifactId>
              </dependency>
              <dependency>
                <groupId>org.hsqldb</groupId>
                <artifactId>hsqldb</artifactId>
              </dependency>
              <dependency>
                <groupId>org.jsoup</groupId>
                <artifactId>jsoup</artifactId>
              </dependency>
              <dependency>
                <groupId>com.nulab-inc</groupId>
                <artifactId>zxcvbn</artifactId>
              </dependency>
              <dependency>
                <groupId>com.thoughtworks.xstream</groupId>
                <artifactId>xstream</artifactId>
              </dependency>
              <dependency>
                <groupId>cglib</groupId>
                <artifactId>cglib-nodep</artifactId>
              </dependency>
              <dependency>
                <groupId>xml-resolver</groupId>
                <artifactId>xml-resolver</artifactId>
              </dependency>
              <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt</artifactId>
              </dependency>
              <dependency>
                <groupId>com.google.guava</groupId>
                <artifactId>guava</artifactId>
              </dependency>
              <dependency>
                <groupId>commons-io</groupId>
                <artifactId>commons-io</artifactId>
              </dependency>
              <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-lang3</artifactId>
              </dependency>
              <dependency>
                <groupId>org.apache.commons</groupId>
                <artifactId>commons-text</artifactId>
              </dependency>
              <dependency>
                <groupId>org.bitbucket.b_c</groupId>
                <artifactId>jose4j</artifactId>
              </dependency>
              <dependency>
                <groupId>org.webjars</groupId>
                <artifactId>bootstrap</artifactId>
              </dependency>
              <dependency>
                <groupId>org.webjars</groupId>
                <artifactId>jquery</artifactId>
              </dependency>
              <dependency>
                <groupId>jakarta.xml.bind</groupId>
                <artifactId>jakarta.xml.bind-api</artifactId>
              </dependency>
              <dependency>
                <groupId>com.sun.xml.bind</groupId>
                <artifactId>jaxb-impl</artifactId>
                <scope>runtime</scope>
              </dependency>

              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-test</artifactId>
                <scope>test</scope>
              </dependency>
              <dependency>
                <groupId>org.springframework.security</groupId>
                <artifactId>spring-security-test</artifactId>
                <scope>test</scope>
              </dependency>
              <dependency>
                <groupId>com.github.tomakehurst</groupId>
                <artifactId>wiremock</artifactId>
                <version>3.0.0-beta-2</version>
                <scope>test</scope>
              </dependency>
              <dependency>
                <groupId>io.rest-assured</groupId>
                <artifactId>rest-assured</artifactId>
                <scope>test</scope>
              </dependency>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-properties-migrator</artifactId>
                <scope>runtime</scope>
              </dependency>
            </dependencies>

            <repositories>
              <repository>
                <snapshots>
                  <enabled>false</enabled>
                </snapshots>
                <id>central</id>
                <url>https://repo.maven.apache.org/maven2</url>
              </repository>
            </repositories>
            <pluginRepositories>
              <pluginRepository>
                <snapshots>
                  <enabled>false</enabled>
                </snapshots>
                <id>central</id>
                <url>https://repo.maven.apache.org/maven2</url>
              </pluginRepository>
            </pluginRepositories>

            <build>
              <plugins>
                <plugin>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-maven-plugin</artifactId>
                  <configuration>
                    <excludeDevtools>true</excludeDevtools>
                    <executable>true</executable>
                    <mainClass>org.owasp.webgoat.server.StartWebGoat</mainClass>
                    <!-- See http://docs.spring.io/spring-boot/docs/current/reference/html/howto-build.html#howto-extract-specific-libraries-when-an-executable-jar-runs -->
                    <requiresUnpack>
                      <dependency>
                        <groupId>org.asciidoctor</groupId>
                        <artifactId>asciidoctorj</artifactId>
                      </dependency>
                    </requiresUnpack>
                  </configuration>
                  <executions>
                    <execution>
                      <goals>
                        <goal>repackage</goal>
                      </goals>
                    </execution>
                  </executions>
                </plugin>
                <plugin>
                  <groupId>org.codehaus.mojo</groupId>
                  <artifactId>build-helper-maven-plugin</artifactId>
                  <executions>
                    <execution>
                      <id>add-integration-test-source-as-test-sources</id>
                      <goals>
                        <goal>add-test-source</goal>
                      </goals>
                      <phase>generate-test-sources</phase>
                      <configuration>
                        <sources>
                          <source>src/it/java</source>
                        </sources>
                      </configuration>
                    </execution>
                  </executions>
                </plugin>
                <plugin>
                  <groupId>org.apache.maven.plugins</groupId>
                  <artifactId>maven-failsafe-plugin</artifactId>
                  <configuration>
                    <systemPropertyVariables>
                      <logback.configurationFile>${basedir}/src/test/resources/logback-test.xml</logback.configurationFile>
                    </systemPropertyVariables>
                    <argLine>-Xmx512m -Dwebgoatport=${webgoat.port} -Dwebwolfport=${webwolf.port}</argLine>
                    <includes>org/owasp/webgoat/*Test</includes>
                  </configuration>
                  <executions>
                    <execution>
                      <id>integration-test</id>
                      <goals>
                        <goal>integration-test</goal>
                      </goals>
                    </execution>
                    <execution>
                      <id>verify</id>
                      <goals>
                        <goal>verify</goal>
                      </goals>
                    </execution>
                  </executions>
                </plugin>
                <plugin>
                  <groupId>org.apache.maven.plugins</groupId>
                  <artifactId>maven-surefire-plugin</artifactId>
                  <version>${maven-surefire-plugin.version}</version>
                  <configuration>
                    <argLine>--add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
                    --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
                    --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED
                    --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED
                    --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED</argLine>
                    <excludes>
                      <exclude>**/*IntegrationTest.java</exclude>
                      <exclude>src/it/java</exclude>
                      <exclude>org/owasp/webgoat/*Test</exclude>
                    </excludes>
                  </configuration>
                </plugin>
                <plugin>
                  <groupId>org.apache.maven.plugins</groupId>
                  <artifactId>maven-checkstyle-plugin</artifactId>
                  <version>${checkstyle.version}</version>
                  <configuration>
                    <encoding>UTF-8</encoding>
                    <consoleOutput>true</consoleOutput>
                    <failsOnError>true</failsOnError>
                    <configLocation>config/checkstyle/checkstyle.xml</configLocation>
                    <suppressionsLocation>config/checkstyle/suppressions.xml</suppressionsLocation>
                    <suppressionsFileExpression>checkstyle.suppressions.file</suppressionsFileExpression>
                  </configuration>
                </plugin>
                <plugin>
                  <groupId>com.diffplug.spotless</groupId>
                  <artifactId>spotless-maven-plugin</artifactId>
                  <version>2.38.0</version>
                  <configuration>
                    <formats>
                      <format>
                        <includes>
                          <include>.gitignore</include>
                        </includes>
                        <trimTrailingWhitespace></trimTrailingWhitespace>
                        <endWithNewline></endWithNewline>
                        <indent>
                          <tabs>true</tabs>
                          <spacesPerTab>4</spacesPerTab>
                        </indent>
                      </format>
                    </formats>
                    <markdown>
                      <includes>
                        <include>**/*.md</include>
                      </includes>
                      <flexmark></flexmark>
                    </markdown>
                    <java>
                      <includes>
                        <include>src/main/java/**/*.java</include>
                        <include>src/test/java/**/*.java</include>
                        <include>src/it/java/**/*.java</include>
                      </includes>
                      <removeUnusedImports></removeUnusedImports>
                      <googleJavaFormat>
                        <style>GOOGLE</style>
                        <reflowLongStrings>true</reflowLongStrings>
                      </googleJavaFormat>
                    </java>
                    <pom>
                      <sortPom>
                        <encoding>UTF-8</encoding>
                        <lineSeparator>${line.separator}</lineSeparator>
                        <expandEmptyElements>true</expandEmptyElements>
                        <spaceBeforeCloseEmptyElement>false</spaceBeforeCloseEmptyElement>
                        <keepBlankLines>true</keepBlankLines>
                        <nrOfIndentSpace>2</nrOfIndentSpace>
                        <indentBlankLines>false</indentBlankLines>
                        <indentSchemaLocation>false</indentSchemaLocation>
                        <predefinedSortOrder>recommended_2008_06</predefinedSortOrder>
                        <sortProperties>true</sortProperties>
                        <sortModules>true</sortModules>
                        <sortExecutions>true</sortExecutions>
                      </sortPom>
                    </pom>
                  </configuration>
                  <executions>
                    <execution>
                      <goals>
                        <goal>check</goal>
                      </goals>
                    </execution>
                  </executions>
                </plugin>
                <plugin>
                  <groupId>org.apache.maven.plugins</groupId>
                  <artifactId>maven-enforcer-plugin</artifactId>
                  <version>3.3.0</version>
                  <executions>
                    <execution>
                      <id>restrict-log4j-versions</id>
                      <goals>
                        <goal>enforce</goal>
                      </goals>
                      <phase>validate</phase>
                      <configuration>
                        <rules>
                          <bannedDependencies>
                            <excludes combine.children="append">
                              <exclude>org.apache.logging.log4j:log4j-core</exclude>
                            </excludes>
                          </bannedDependencies>
                        </rules>
                        <fail>true</fail>
                      </configuration>
                    </execution>
                  </executions>
                </plugin>
                <plugin>
                  <groupId>org.apache.maven.plugins</groupId>
                  <artifactId>maven-compiler-plugin</artifactId>
                  <configuration>
                    <source>17</source>
                    <target>17</target>
                  </configuration>
                </plugin>
              </plugins>
            </build>

            <profiles>
              <profile>
                <id>local-server</id>
              </profile>
              <profile>
                <id>start-server</id>
                <activation>
                  <activeByDefault>true</activeByDefault>
                </activation>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.codehaus.mojo</groupId>
                      <artifactId>build-helper-maven-plugin</artifactId>
                      <executions>
                        <execution>
                          <id>reserve-container-port</id>
                          <goals>
                            <goal>reserve-network-port</goal>
                          </goals>
                          <phase>process-resources</phase>
                          <configuration>
                            <portNames>
                              <portName>webgoat.port</portName>
                              <portName>webwolf.port</portName>
                              <portName>jmxPort</portName>
                            </portNames>
                          </configuration>
                        </execution>
                      </executions>
                    </plugin>
                    <plugin>
                      <groupId>com.bazaarvoice.maven.plugins</groupId>
                      <artifactId>process-exec-maven-plugin</artifactId>
                      <version>0.9</version>
                      <executions>
                        <execution>
                          <id>start-jar</id>
                          <goals>
                            <goal>start</goal>
                          </goals>
                          <phase>pre-integration-test</phase>
                          <configuration>
                            <workingDir>${project.build.directory}</workingDir>
                            <arguments>
                              <argument>java</argument>
                              <argument>-jar</argument>
                              <argument>-Dlogging.pattern.console=</argument>
                              <argument>-Dwebgoat.server.directory=${java.io.tmpdir}/webgoat_${webgoat.port}</argument>
                              <argument>-Dwebgoat.user.directory=${java.io.tmpdir}/webgoat_${webgoat.port}</argument>
                              <argument>-Dspring.main.banner-mode=off</argument>
                              <argument>-Dwebgoat.port=${webgoat.port}</argument>
                              <argument>-Dwebwolf.port=${webwolf.port}</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/java.lang=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/java.util=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/java.lang.reflect=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/java.text=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.desktop/java.beans=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.desktop/java.awt.font=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/sun.nio.ch=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/java.io=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/java.util=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/sun.nio.ch=ALL-UNNAMED</argument>
                              <argument>--add-opens</argument>
                              <argument>java.base/java.io=ALL-UNNAMED</argument>
                              <argument>${project.build.directory}/webgoat-${project.version}.jar</argument>
                            </arguments>
                            <waitForInterrupt>false</waitForInterrupt>
                            <healthcheckUrl>http://localhost:${webgoat.port}/WebGoat/actuator/health</healthcheckUrl>
                          </configuration>
                        </execution>
                        <execution>
                          <id>stop-jar-process</id>
                          <goals>
                            <goal>stop-all</goal>
                          </goals>
                          <phase>post-integration-test</phase>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </profile>
              <profile>
                <id>owasp</id>
                <activation>
                  <activeByDefault>false</activeByDefault>
                </activation>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.owasp</groupId>
                      <artifactId>dependency-check-maven</artifactId>
                      <version>6.5.1</version>
                      <configuration>
                        <failBuildOnCVSS>7</failBuildOnCVSS>
                        <skipProvidedScope>false</skipProvidedScope>
                        <skipRuntimeScope>false</skipRuntimeScope>
                        <suppressionFiles>
                          <!--suppress UnresolvedMavenProperty -->
                          <suppressionFile>${maven.multiModuleProjectDirectory}/config/dependency-check/project-suppression.xml</suppressionFile>
                        </suppressionFiles>
                      </configuration>
                      <executions>
                        <execution>
                          <goals>
                            <goal>check</goal>
                          </goals>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </profile>
              <profile>
                <!-- run with: mvn test -Pcoverage -->
                <id>coverage</id>
                <activation>
                  <activeByDefault>false</activeByDefault>
                </activation>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.apache.maven.plugins</groupId>
                      <artifactId>maven-surefire-plugin</artifactId>
                      <version>${maven-surefire-plugin.version}</version>
                      <configuration>
                        <argLine>--add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
                    --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
                    --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED
                    --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED
                    ${surefire.jacoco.args}</argLine>
                        <excludes>
                          <exclude>**/*IntegrationTest.java</exclude>
                          <exclude>src/it/java</exclude>
                          <exclude>org/owasp/webgoat/*Test</exclude>
                        </excludes>
                      </configuration>
                    </plugin>
                    <plugin>
                      <groupId>org.jacoco</groupId>
                      <artifactId>jacoco-maven-plugin</artifactId>
                      <version>${jacoco.version}</version>
                      <executions>
                        <execution>
                          <id>before-unit-test</id>
                          <goals>
                            <goal>prepare-agent</goal>
                          </goals>
                          <configuration>
                            <destFile>${project.build.directory}/jacoco/jacoco-ut.exec</destFile>
                            <propertyName>surefire.jacoco.args</propertyName>
                          </configuration>
                        </execution>
                        <execution>
                          <id>check</id>
                          <goals>
                            <goal>check</goal>
                          </goals>
                          <configuration>
                            <rules>
                              <rule>
                                <element>BUNDLE</element>
                                <limits>
                                  <limit>
                                    <counter>CLASS</counter>
                                    <value>COVEREDCOUNT</value>
                                    <minimum>0.6</minimum>
                                  </limit>
                                </limits>
                              </rule>
                            </rules>
                            <dataFile>${project.build.directory}/jacoco/jacoco-ut.exec</dataFile>
                          </configuration>
                        </execution>
                        <execution>
                          <id>after-unit-test</id>
                          <goals>
                            <goal>report</goal>
                          </goals>
                          <phase>test</phase>
                          <configuration>
                            <dataFile>${project.build.directory}/jacoco/jacoco-ut.exec</dataFile>
                            <outputDirectory>${project.reporting.outputDirectory}/jacoco-unit-test-coverage-report</outputDirectory>
                          </configuration>
                        </execution>
                      </executions>
                    </plugin>
                  </plugins>
                </build>
              </profile>
            </profiles>
          </project>
                  """;

  private static final String webgoatPomThatJustNeedsUpgrades =
      """
                  <?xml version="1.0" encoding="UTF-8"?>
                  <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>

                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>3.1.0</version>
                    </parent>

                    <groupId>org.owasp.webgoat</groupId>
                    <artifactId>webgoat</artifactId>
                    <version>2023.5-SNAPSHOT</version>
                    <packaging>jar</packaging>

                    <name>WebGoat</name>
                    <description>WebGoat, a deliberately insecure Web Application</description>
                    <url>https://github.com/WebGoat/WebGoat</url>
                    <inceptionYear>2006</inceptionYear>
                    <organization>
                      <name>OWASP</name>
                      <url>https://github.com/WebGoat/WebGoat/</url>
                    </organization>
                    <licenses>
                      <license>
                        <name>GNU General Public License, version 2</name>
                        <url>https://www.gnu.org/licenses/gpl-2.0.txt</url>
                      </license>
                    </licenses>

                    <developers>
                      <developer>
                        <id>mayhew64</id>
                        <name>Bruce Mayhew</name>
                        <email>webgoat@owasp.org</email>
                        <organization>OWASP</organization>
                        <organizationUrl>https://github.com/WebGoat/WebGoat</organizationUrl>
                      </developer>
                      <developer>
                        <id>nbaars</id>
                        <name>Nanne Baars</name>
                        <email>nanne.baars@owasp.org</email>
                        <organizationUrl>https://github.com/nbaars</organizationUrl>
                        <timezone>Europe/Amsterdam</timezone>
                      </developer>
                      <developer>
                        <id>misfir3</id>
                        <name>Jason White</name>
                        <email>jason.white@owasp.org</email>
                      </developer>
                      <developer>
                        <id>zubcevic</id>
                        <name>René Zubcevic</name>
                        <email>rene.zubcevic@owasp.org</email>
                      </developer>
                      <developer>
                        <id>aolle</id>
                        <name>Àngel Ollé Blázquez</name>
                        <email>angel@olleb.com</email>
                      </developer>
                      <developer>
                        <id>jwayman</id>
                        <name>Jeff Wayman</name>
                        <email></email>
                      </developer>
                      <developer>
                        <id>dcowden</id>
                        <name>Dave Cowden</name>
                        <email></email>
                      </developer>
                      <developer>
                        <id>lawson89</id>
                        <name>Richard Lawson</name>
                        <email></email>
                      </developer>
                      <developer>
                        <id>dougmorato</id>
                        <name>Doug Morato</name>
                        <email>doug.morato@owasp.org</email>
                        <organization>OWASP</organization>
                        <organizationUrl>https://github.com/dougmorato</organizationUrl>
                        <timezone>America/New_York</timezone>
                        <properties>
                          <picUrl>https://avatars2.githubusercontent.com/u/9654?v=3&amp;s=150</picUrl>
                        </properties>
                      </developer>
                    </developers>

                    <mailingLists>
                      <mailingList>
                        <name>OWASP WebGoat Mailing List</name>
                        <subscribe>https://lists.owasp.org/mailman/listinfo/owasp-webgoat</subscribe>
                        <unsubscribe>Owasp-webgoat-request@lists.owasp.org</unsubscribe>
                        <post>owasp-webgoat@lists.owasp.org</post>
                        <archive>http://lists.owasp.org/pipermail/owasp-webgoat/</archive>
                      </mailingList>
                    </mailingLists>
                    <scm>
                      <connection>scm:git:git@github.com:WebGoat/WebGoat.git</connection>
                      <developerConnection>scm:git:git@github.com:WebGoat/WebGoat.git</developerConnection>
                      <tag>HEAD</tag>
                      <url>https://github.com/WebGoat/WebGoat</url>
                    </scm>

                    <issueManagement>
                      <system>Github Issues</system>
                      <url>https://github.com/WebGoat/WebGoat/issues</url>
                    </issueManagement>

                    <properties>
                      <!-- Shared properties with plugins and version numbers across submodules-->
                      <asciidoctorj.version>2.5.10</asciidoctorj.version>
                      <!-- Upgrading needs UI work in WebWolf -->
                      <bootstrap.version>3.3.7</bootstrap.version>
                      <cglib.version>3.3.0</cglib.version>
                      <!-- do not update necessary for lesson -->
                      <checkstyle.version>3.3.0</checkstyle.version>
                      <commons-collections.version>3.2.1</commons-collections.version>
                      <commons-io.version>2.11.0</commons-io.version>
                      <commons-lang3.version>3.12.0</commons-lang3.version>
                      <commons-text.version>1.10.0</commons-text.version>
                      <guava.version>32.1.1-jre</guava.version>
                      <jacoco.version>0.8.10</jacoco.version>
                      <java.version>17</java.version>
                      <jaxb.version>2.3.1</jaxb.version>
                      <jjwt.version>0.9.1</jjwt.version>
                      <jose4j.version>0.9.3</jose4j.version>
                      <jquery.version>3.6.4</jquery.version>
                      <jsoup.version>1.16.1</jsoup.version>
                      <maven-compiler-plugin.version>3.8.0</maven-compiler-plugin.version>
                      <maven-failsafe-plugin.version>2.22.0</maven-failsafe-plugin.version>
                      <maven-jar-plugin.version>3.1.2</maven-jar-plugin.version>
                      <maven-javadoc-plugin.version>3.1.1</maven-javadoc-plugin.version>
                      <maven-source-plugin.version>3.1.0</maven-source-plugin.version>
                      <maven-surefire-plugin.version>3.1.2</maven-surefire-plugin.version>
                      <maven.compiler.source>17</maven.compiler.source>
                      <maven.compiler.target>17</maven.compiler.target>
                      <pmd.version>3.15.0</pmd.version>
                      <!-- Use UTF-8 Encoding -->
                      <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                      <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
                      <thymeleaf.version>3.1.1.RELEASE</thymeleaf.version>
                      <webdriver.version>5.3.2</webdriver.version>
                      <webgoat.port>8080</webgoat.port>
                      <webwolf.port>9090</webwolf.port>
                      <wiremock.version>2.27.2</wiremock.version>
                      <xml-resolver.version>1.2</xml-resolver.version>
                      <xstream.version>1.4.5</xstream.version>
                      <!-- do not update necessary for lesson -->
                      <zxcvbn.version>1.8.0</zxcvbn.version>
                      <versions.java-security-toolkit>1.0.6</versions.java-security-toolkit>
                    </properties>

                    <dependencyManagement>
                      <dependencies>

                        <dependency>
                          <groupId>org.ow2.asm</groupId>
                          <artifactId>asm</artifactId>
                          <version>9.5</version>
                        </dependency>

                        <dependency>
                          <groupId>org.apache.commons</groupId>
                          <artifactId>commons-exec</artifactId>
                          <version>1.3</version>
                        </dependency>
                        <dependency>
                          <groupId>org.asciidoctor</groupId>
                          <artifactId>asciidoctorj</artifactId>
                          <version>${asciidoctorj.version}</version>
                        </dependency>
                        <dependency>
                          <!-- jsoup HTML parser library @ https://jsoup.org/ -->
                          <groupId>org.jsoup</groupId>
                          <artifactId>jsoup</artifactId>
                          <version>${jsoup.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>com.nulab-inc</groupId>
                          <artifactId>zxcvbn</artifactId>
                          <version>${zxcvbn.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>com.thoughtworks.xstream</groupId>
                          <artifactId>xstream</artifactId>
                          <version>${xstream.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>cglib</groupId>
                          <artifactId>cglib-nodep</artifactId>
                          <version>${cglib.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>xml-resolver</groupId>
                          <artifactId>xml-resolver</artifactId>
                          <version>${xml-resolver.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>io.jsonwebtoken</groupId>
                          <artifactId>jjwt</artifactId>
                          <version>${jjwt.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>${guava.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>commons-io</groupId>
                          <artifactId>commons-io</artifactId>
                          <version>${commons-io.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>org.apache.commons</groupId>
                          <artifactId>commons-text</artifactId>
                          <version>${commons-text.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>org.bitbucket.b_c</groupId>
                          <artifactId>jose4j</artifactId>
                          <version>${jose4j.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>org.webjars</groupId>
                          <artifactId>bootstrap</artifactId>
                          <version>${bootstrap.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>org.webjars</groupId>
                          <artifactId>jquery</artifactId>
                          <version>${jquery.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>com.github.tomakehurst</groupId>
                          <artifactId>wiremock</artifactId>
                          <version>${wiremock.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>io.github.bonigarcia</groupId>
                          <artifactId>webdrivermanager</artifactId>
                          <version>${webdriver.version}</version>
                        </dependency>
                        <dependency>
                          <groupId>org.apache.commons</groupId>
                          <artifactId>commons-compress</artifactId>
                          <version>1.23.0</version>
                        </dependency>
                        <dependency>
                          <groupId>org.jruby</groupId>
                          <artifactId>jruby</artifactId>
                          <version>9.4.2.0</version>
                        </dependency>
                        <dependency>
                          <groupId>io.github.pixee</groupId>
                          <artifactId>java-security-toolkit</artifactId>
                          <version>${versions.java-security-toolkit}</version>
                        </dependency>
                      </dependencies>
                    </dependencyManagement>
                    <dependencies>
                      <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-exec</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-validation</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <scope>provided</scope>
                        <optional>true</optional>
                      </dependency>
                      <dependency>
                        <groupId>javax.xml.bind</groupId>
                        <artifactId>jaxb-api</artifactId>
                        <version>${jaxb.version}</version>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-undertow</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                        <exclusions>
                          <exclusion>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-tomcat</artifactId>
                          </exclusion>
                        </exclusions>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-actuator</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.flywaydb</groupId>
                        <artifactId>flyway-core</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.asciidoctor</groupId>
                        <artifactId>asciidoctorj</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-data-jpa</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-security</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-thymeleaf</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.thymeleaf.extras</groupId>
                        <artifactId>thymeleaf-extras-springsecurity6</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>jakarta.servlet</groupId>
                        <artifactId>jakarta.servlet-api</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.hsqldb</groupId>
                        <artifactId>hsqldb</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.jsoup</groupId>
                        <artifactId>jsoup</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>com.nulab-inc</groupId>
                        <artifactId>zxcvbn</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>com.thoughtworks.xstream</groupId>
                        <artifactId>xstream</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>cglib</groupId>
                        <artifactId>cglib-nodep</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>xml-resolver</groupId>
                        <artifactId>xml-resolver</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>io.jsonwebtoken</groupId>
                        <artifactId>jjwt</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>commons-io</groupId>
                        <artifactId>commons-io</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-lang3</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.apache.commons</groupId>
                        <artifactId>commons-text</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.bitbucket.b_c</groupId>
                        <artifactId>jose4j</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.webjars</groupId>
                        <artifactId>bootstrap</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>org.webjars</groupId>
                        <artifactId>jquery</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>jakarta.xml.bind</groupId>
                        <artifactId>jakarta.xml.bind-api</artifactId>
                      </dependency>
                      <dependency>
                        <groupId>com.sun.xml.bind</groupId>
                        <artifactId>jaxb-impl</artifactId>
                        <scope>runtime</scope>
                      </dependency>

                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-test</artifactId>
                        <scope>test</scope>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.security</groupId>
                        <artifactId>spring-security-test</artifactId>
                        <scope>test</scope>
                      </dependency>
                      <dependency>
                        <groupId>com.github.tomakehurst</groupId>
                        <artifactId>wiremock</artifactId>
                        <version>3.0.0-beta-2</version>
                        <scope>test</scope>
                      </dependency>
                      <dependency>
                        <groupId>io.rest-assured</groupId>
                        <artifactId>rest-assured</artifactId>
                        <scope>test</scope>
                      </dependency>
                      <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-properties-migrator</artifactId>
                        <scope>runtime</scope>
                      </dependency>
                      <dependency>
                        <groupId>io.github.pixee</groupId>
                        <artifactId>java-security-toolkit</artifactId>
                      </dependency>
                    </dependencies>

                    <repositories>
                      <repository>
                        <snapshots>
                          <enabled>false</enabled>
                        </snapshots>
                        <id>central</id>
                        <url>https://repo.maven.apache.org/maven2</url>
                      </repository>
                    </repositories>
                    <pluginRepositories>
                      <pluginRepository>
                        <snapshots>
                          <enabled>false</enabled>
                        </snapshots>
                        <id>central</id>
                        <url>https://repo.maven.apache.org/maven2</url>
                      </pluginRepository>
                    </pluginRepositories>

                    <build>
                      <plugins>
                        <plugin>
                          <groupId>org.springframework.boot</groupId>
                          <artifactId>spring-boot-maven-plugin</artifactId>
                          <configuration>
                            <excludeDevtools>true</excludeDevtools>
                            <executable>true</executable>
                            <mainClass>org.owasp.webgoat.server.StartWebGoat</mainClass>
                            <!-- See http://docs.spring.io/spring-boot/docs/current/reference/html/howto-build.html#howto-extract-specific-libraries-when-an-executable-jar-runs -->
                            <requiresUnpack>
                              <dependency>
                                <groupId>org.asciidoctor</groupId>
                                <artifactId>asciidoctorj</artifactId>
                              </dependency>
                            </requiresUnpack>
                          </configuration>
                          <executions>
                            <execution>
                              <goals>
                                <goal>repackage</goal>
                              </goals>
                            </execution>
                          </executions>
                        </plugin>
                        <plugin>
                          <groupId>org.codehaus.mojo</groupId>
                          <artifactId>build-helper-maven-plugin</artifactId>
                          <executions>
                            <execution>
                              <id>add-integration-test-source-as-test-sources</id>
                              <goals>
                                <goal>add-test-source</goal>
                              </goals>
                              <phase>generate-test-sources</phase>
                              <configuration>
                                <sources>
                                  <source>src/it/java</source>
                                </sources>
                              </configuration>
                            </execution>
                          </executions>
                        </plugin>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-failsafe-plugin</artifactId>
                          <configuration>
                            <systemPropertyVariables>
                              <logback.configurationFile>${basedir}/src/test/resources/logback-test.xml</logback.configurationFile>
                            </systemPropertyVariables>
                            <argLine>-Xmx512m -Dwebgoatport=${webgoat.port} -Dwebwolfport=${webwolf.port}</argLine>
                            <includes>org/owasp/webgoat/*Test</includes>
                          </configuration>
                          <executions>
                            <execution>
                              <id>integration-test</id>
                              <goals>
                                <goal>integration-test</goal>
                              </goals>
                            </execution>
                            <execution>
                              <id>verify</id>
                              <goals>
                                <goal>verify</goal>
                              </goals>
                            </execution>
                          </executions>
                        </plugin>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-surefire-plugin</artifactId>
                          <version>${maven-surefire-plugin.version}</version>
                          <configuration>
                            <argLine>--add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
                            --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
                            --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED
                            --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED
                            --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED</argLine>
                            <excludes>
                              <exclude>**/*IntegrationTest.java</exclude>
                              <exclude>src/it/java</exclude>
                              <exclude>org/owasp/webgoat/*Test</exclude>
                            </excludes>
                          </configuration>
                        </plugin>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-checkstyle-plugin</artifactId>
                          <version>${checkstyle.version}</version>
                          <configuration>
                            <encoding>UTF-8</encoding>
                            <consoleOutput>true</consoleOutput>
                            <failsOnError>true</failsOnError>
                            <configLocation>config/checkstyle/checkstyle.xml</configLocation>
                            <suppressionsLocation>config/checkstyle/suppressions.xml</suppressionsLocation>
                            <suppressionsFileExpression>checkstyle.suppressions.file</suppressionsFileExpression>
                          </configuration>
                        </plugin>
                        <plugin>
                          <groupId>com.diffplug.spotless</groupId>
                          <artifactId>spotless-maven-plugin</artifactId>
                          <version>2.38.0</version>
                          <configuration>
                            <formats>
                              <format>
                                <includes>
                                  <include>.gitignore</include>
                                </includes>
                                <trimTrailingWhitespace></trimTrailingWhitespace>
                                <endWithNewline></endWithNewline>
                                <indent>
                                  <tabs>true</tabs>
                                  <spacesPerTab>4</spacesPerTab>
                                </indent>
                              </format>
                            </formats>
                            <markdown>
                              <includes>
                                <include>**/*.md</include>
                              </includes>
                              <flexmark></flexmark>
                            </markdown>
                            <java>
                              <includes>
                                <include>src/main/java/**/*.java</include>
                                <include>src/test/java/**/*.java</include>
                                <include>src/it/java/**/*.java</include>
                              </includes>
                              <removeUnusedImports></removeUnusedImports>
                              <googleJavaFormat>
                                <style>GOOGLE</style>
                                <reflowLongStrings>true</reflowLongStrings>
                              </googleJavaFormat>
                            </java>
                            <pom>
                              <sortPom>
                                <encoding>UTF-8</encoding>
                                <lineSeparator>${line.separator}</lineSeparator>
                                <expandEmptyElements>true</expandEmptyElements>
                                <spaceBeforeCloseEmptyElement>false</spaceBeforeCloseEmptyElement>
                                <keepBlankLines>true</keepBlankLines>
                                <nrOfIndentSpace>2</nrOfIndentSpace>
                                <indentBlankLines>false</indentBlankLines>
                                <indentSchemaLocation>false</indentSchemaLocation>
                                <predefinedSortOrder>recommended_2008_06</predefinedSortOrder>
                                <sortProperties>true</sortProperties>
                                <sortModules>true</sortModules>
                                <sortExecutions>true</sortExecutions>
                              </sortPom>
                            </pom>
                          </configuration>
                          <executions>
                            <execution>
                              <goals>
                                <goal>check</goal>
                              </goals>
                            </execution>
                          </executions>
                        </plugin>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-enforcer-plugin</artifactId>
                          <version>3.3.0</version>
                          <executions>
                            <execution>
                              <id>restrict-log4j-versions</id>
                              <goals>
                                <goal>enforce</goal>
                              </goals>
                              <phase>validate</phase>
                              <configuration>
                                <rules>
                                  <bannedDependencies>
                                    <excludes combine.children="append">
                                      <exclude>org.apache.logging.log4j:log4j-core</exclude>
                                    </excludes>
                                  </bannedDependencies>
                                </rules>
                                <fail>true</fail>
                              </configuration>
                            </execution>
                          </executions>
                        </plugin>
                        <plugin>
                          <groupId>org.apache.maven.plugins</groupId>
                          <artifactId>maven-compiler-plugin</artifactId>
                          <configuration>
                            <source>17</source>
                            <target>17</target>
                          </configuration>
                        </plugin>
                      </plugins>
                    </build>

                    <profiles>
                      <profile>
                        <id>local-server</id>
                      </profile>
                      <profile>
                        <id>start-server</id>
                        <activation>
                          <activeByDefault>true</activeByDefault>
                        </activation>
                        <build>
                          <plugins>
                            <plugin>
                              <groupId>org.codehaus.mojo</groupId>
                              <artifactId>build-helper-maven-plugin</artifactId>
                              <executions>
                                <execution>
                                  <id>reserve-container-port</id>
                                  <goals>
                                    <goal>reserve-network-port</goal>
                                  </goals>
                                  <phase>process-resources</phase>
                                  <configuration>
                                    <portNames>
                                      <portName>webgoat.port</portName>
                                      <portName>webwolf.port</portName>
                                      <portName>jmxPort</portName>
                                    </portNames>
                                  </configuration>
                                </execution>
                              </executions>
                            </plugin>
                            <plugin>
                              <groupId>com.bazaarvoice.maven.plugins</groupId>
                              <artifactId>process-exec-maven-plugin</artifactId>
                              <version>0.9</version>
                              <executions>
                                <execution>
                                  <id>start-jar</id>
                                  <goals>
                                    <goal>start</goal>
                                  </goals>
                                  <phase>pre-integration-test</phase>
                                  <configuration>
                                    <workingDir>${project.build.directory}</workingDir>
                                    <arguments>
                                      <argument>java</argument>
                                      <argument>-jar</argument>
                                      <argument>-Dlogging.pattern.console=</argument>
                                      <argument>-Dwebgoat.server.directory=${java.io.tmpdir}/webgoat_${webgoat.port}</argument>
                                      <argument>-Dwebgoat.user.directory=${java.io.tmpdir}/webgoat_${webgoat.port}</argument>
                                      <argument>-Dspring.main.banner-mode=off</argument>
                                      <argument>-Dwebgoat.port=${webgoat.port}</argument>
                                      <argument>-Dwebwolf.port=${webwolf.port}</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/java.lang=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/java.util=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/java.lang.reflect=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/java.text=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.desktop/java.beans=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.desktop/java.awt.font=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/sun.nio.ch=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/java.io=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/java.util=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/sun.nio.ch=ALL-UNNAMED</argument>
                                      <argument>--add-opens</argument>
                                      <argument>java.base/java.io=ALL-UNNAMED</argument>
                                      <argument>${project.build.directory}/webgoat-${project.version}.jar</argument>
                                    </arguments>
                                    <waitForInterrupt>false</waitForInterrupt>
                                    <healthcheckUrl>http://localhost:${webgoat.port}/WebGoat/actuator/health</healthcheckUrl>
                                  </configuration>
                                </execution>
                                <execution>
                                  <id>stop-jar-process</id>
                                  <goals>
                                    <goal>stop-all</goal>
                                  </goals>
                                  <phase>post-integration-test</phase>
                                </execution>
                              </executions>
                            </plugin>
                          </plugins>
                        </build>
                      </profile>
                      <profile>
                        <id>owasp</id>
                        <activation>
                          <activeByDefault>false</activeByDefault>
                        </activation>
                        <build>
                          <plugins>
                            <plugin>
                              <groupId>org.owasp</groupId>
                              <artifactId>dependency-check-maven</artifactId>
                              <version>6.5.1</version>
                              <configuration>
                                <failBuildOnCVSS>7</failBuildOnCVSS>
                                <skipProvidedScope>false</skipProvidedScope>
                                <skipRuntimeScope>false</skipRuntimeScope>
                                <suppressionFiles>
                                  <!--suppress UnresolvedMavenProperty -->
                                  <suppressionFile>${maven.multiModuleProjectDirectory}/config/dependency-check/project-suppression.xml</suppressionFile>
                                </suppressionFiles>
                              </configuration>
                              <executions>
                                <execution>
                                  <goals>
                                    <goal>check</goal>
                                  </goals>
                                </execution>
                              </executions>
                            </plugin>
                          </plugins>
                        </build>
                      </profile>
                      <profile>
                        <!-- run with: mvn test -Pcoverage -->
                        <id>coverage</id>
                        <activation>
                          <activeByDefault>false</activeByDefault>
                        </activation>
                        <build>
                          <plugins>
                            <plugin>
                              <groupId>org.apache.maven.plugins</groupId>
                              <artifactId>maven-surefire-plugin</artifactId>
                              <version>${maven-surefire-plugin.version}</version>
                              <configuration>
                                <argLine>--add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
                            --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.base/java.io=ALL-UNNAMED
                            --add-opens java.base/java.util=ALL-UNNAMED --add-opens java.base/java.lang.reflect=ALL-UNNAMED
                            --add-opens java.base/java.text=ALL-UNNAMED --add-opens java.desktop/java.awt.font=ALL-UNNAMED
                            ${surefire.jacoco.args}</argLine>
                                <excludes>
                                  <exclude>**/*IntegrationTest.java</exclude>
                                  <exclude>src/it/java</exclude>
                                  <exclude>org/owasp/webgoat/*Test</exclude>
                                </excludes>
                              </configuration>
                            </plugin>
                            <plugin>
                              <groupId>org.jacoco</groupId>
                              <artifactId>jacoco-maven-plugin</artifactId>
                              <version>${jacoco.version}</version>
                              <executions>
                                <execution>
                                  <id>before-unit-test</id>
                                  <goals>
                                    <goal>prepare-agent</goal>
                                  </goals>
                                  <configuration>
                                    <destFile>${project.build.directory}/jacoco/jacoco-ut.exec</destFile>
                                    <propertyName>surefire.jacoco.args</propertyName>
                                  </configuration>
                                </execution>
                                <execution>
                                  <id>check</id>
                                  <goals>
                                    <goal>check</goal>
                                  </goals>
                                  <configuration>
                                    <rules>
                                      <rule>
                                        <element>BUNDLE</element>
                                        <limits>
                                          <limit>
                                            <counter>CLASS</counter>
                                            <value>COVEREDCOUNT</value>
                                            <minimum>0.6</minimum>
                                          </limit>
                                        </limits>
                                      </rule>
                                    </rules>
                                    <dataFile>${project.build.directory}/jacoco/jacoco-ut.exec</dataFile>
                                  </configuration>
                                </execution>
                                <execution>
                                  <id>after-unit-test</id>
                                  <goals>
                                    <goal>report</goal>
                                  </goals>
                                  <phase>test</phase>
                                  <configuration>
                                    <dataFile>${project.build.directory}/jacoco/jacoco-ut.exec</dataFile>
                                    <outputDirectory>${project.reporting.outputDirectory}/jacoco-unit-test-coverage-report</outputDirectory>
                                  </configuration>
                                </execution>
                              </executions>
                            </plugin>
                          </plugins>
                        </build>
                      </profile>
                    </profiles>
                  </project>
                  """;
}
