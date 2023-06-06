package io.codemodder.plugins.maven;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.codemodder.*;
import io.codemodder.codetf.CodeTFChangesetEntry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link MavenProvider}. We bend over backwards here not to test the pom-operator
 * types.
 */
final class MavenProviderTest {

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
  }

  @Test
  void it_returns_changeset_when_no_issues() throws IOException {
    CodeTFChangesetEntry changesetEntry =
        new CodeTFChangesetEntry(marsJavaFile.toString(), "diff here", List.of());
    when(pomFileUpdater.updatePom(eq(projectDir), eq(module1Pom), any()))
        .thenReturn(new PomUpdateResult(Optional.of(changesetEntry), List.of()));

    MavenProvider provider = new MavenProvider(pomFileFinder, pomFileUpdater);
    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir, marsJavaFile, List.of(marsDependency1, marsDependency2));

    // no errors, success!
    assertThat(result.erroredFiles()).isEmpty();

    // we've updated all the poms, so we merge this with the pre-existing one change
    assertThat(result.packageChanges()).containsOnly(changesetEntry);

    // we injected all the dependencies, total success!
    assertThat(result.injectedPackages()).containsOnly(marsDependency1, marsDependency2);
  }

  @Test
  void it_returns_empty_when_no_pom() throws IOException {
    when(pomFileFinder.findForFile(any(), any())).thenReturn(Optional.empty());
    MavenProvider provider = new MavenProvider(pomFileFinder, pomFileUpdater);
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
    pomFileUpdater = new MavenProvider.DefaultPomFileUpdater();
    MavenProvider provider = new MavenProvider(pomFileFinder, pomFileUpdater);
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
    MavenProvider provider = new MavenProvider(pomFileFinder, pomFileUpdater);

    // introduce a failure
    when(pomFileUpdater.updatePom(eq(projectDir), eq(module1Pom), any()))
        .thenThrow(new IOException("failed to update pom"));

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
    PomFileUpdater updater = new MavenProvider.DefaultPomFileUpdater();
    Files.writeString(module1Pom, simplePom);
    PomUpdateResult pomUpdateResult =
        updater.updatePom(
            projectDir, module1Pom, List.of(marsDependency1, marsDependency2, venusDependency));
    Optional<CodeTFChangesetEntry> changesetEntryRef = pomUpdateResult.getEntry();
    assertThat(changesetEntryRef).isPresent();
    CodeTFChangesetEntry changesetEntry = changesetEntryRef.get();
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

  private static final String simplePom =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
          + "    <modelVersion>4.0.0</modelVersion>\n"
          + "    <groupId>link.sharpe</groupId>\n"
          + "    <artifactId>mavenproject1</artifactId>\n"
          + "    <version>1.0-SNAPSHOT</version>\n"
          + "</project>";

  private static final String simplePomAfterChanges =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "  <project xmlns=\"http://maven.apache.org/POM/4.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n"
          + "      <modelVersion>4.0.0</modelVersion>\n"
          + "      <groupId>link.sharpe</groupId>\n"
          + "      <artifactId>mavenproject1</artifactId>\n"
          + "      <version>1.0-SNAPSHOT</version>\n"
          + "      <dependencyManagement>\n"
          + "          <dependencies>\n"
          + "              <dependency>\n"
          + "                  <groupId>com.acme.mars</groupId>\n"
          + "                  <artifactId>mars1</artifactId>\n"
          + "                  <version>${versions.mars1}</version>\n"
          + "              </dependency>\n"
          + "              <dependency>\n"
          + "                  <groupId>com.acme.mars</groupId>\n"
          + "                  <artifactId>mars2</artifactId>\n"
          + "                  <version>${versions.mars2}</version>\n"
          + "              </dependency>\n"
          + "              <dependency>\n"
          + "                  <groupId>com.acme.venus</groupId>\n"
          + "                  <artifactId>venus</artifactId>\n"
          + "                  <version>${versions.venus}</version>\n"
          + "              </dependency>\n"
          + "          </dependencies>\n"
          + "      </dependencyManagement>\n"
          + "      <properties>\n"
          + "          <versions.mars1>1.0.1</versions.mars1>\n"
          + "          <versions.mars2>1.0.1</versions.mars2>\n"
          + "          <versions.venus>1.0.2</versions.venus>\n"
          + "      </properties>\n"
          + "      <dependencies>\n"
          + "          <dependency>\n"
          + "              <groupId>com.acme.mars</groupId>\n"
          + "              <artifactId>mars1</artifactId>\n"
          + "          </dependency>\n"
          + "          <dependency>\n"
          + "              <groupId>com.acme.mars</groupId>\n"
          + "              <artifactId>mars2</artifactId>\n"
          + "          </dependency>\n"
          + "          <dependency>\n"
          + "              <groupId>com.acme.venus</groupId>\n"
          + "              <artifactId>venus</artifactId>\n"
          + "          </dependency>\n"
          + "      </dependencies>\n"
          + "  </project>";
}
