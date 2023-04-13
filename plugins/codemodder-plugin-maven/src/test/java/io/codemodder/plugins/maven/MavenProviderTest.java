package io.codemodder.plugins.maven;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.codemodder.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for {@link MavenProvider}. We bend over backwards here not to test the pom-operator
 * types.
 */
final class MavenProviderTest {

  private PomFileUpdater pomUpdater;
  private PomFileDependencyMapper dependencyMapper;
  private Path projectDir;
  private ChangedFile irrelevant;
  private ChangedFile changedPom1;
  private ChangedFile changedPom2;
  private ChangedFile changedRootPom;
  private FileDependency marsDependency;
  private FileDependency venusDependency;
  private FileDependency jspDependency;

  private Path module1Pom;
  private Path module2Pom;
  private Path module3Pom;
  private Path rootPom;

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

    Path marsJavaFile = this.projectDir.resolve("module1/src/main/java/com/acme/Mars.java");
    Path venusJavaFile = this.projectDir.resolve("module1/src/main/java/com/acme/Venus.java");
    module1Pom = this.projectDir.resolve("module1/pom.xml");

    Path cloud9JavaFile = this.projectDir.resolve("module2/src/main/java/com/acme/Cloud9.java");
    module2Pom = this.projectDir.resolve("module2/pom.xml");

    Path gerrardJavaFile = this.projectDir.resolve("module3/src/main/java/com/acme/Gerrard.java");
    module3Pom = this.projectDir.resolve("module3/pom.xml");

    Path jspFile = this.projectDir.resolve("src/main/resources/webapp/WEB-INF/page.jsp");
    rootPom = this.projectDir.resolve("pom.xml");

    Path irrelevantFile = this.projectDir.resolve("irrelevant");
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
    marsDependency =
        FileDependency.create(
            marsJavaFile,
            List.of(
                DependencyGAV.createDefault("com.acme.mars", "mars1", "1.0.1"),
                DependencyGAV.createDefault("com.acme.mars", "mars2", "1.0.1")));
    venusDependency =
        FileDependency.create(
            venusJavaFile,
            List.of(DependencyGAV.createDefault("com.acme.venus", "venus", "1.0.2")));
    jspDependency =
        FileDependency.create(
            jspFile, List.of(DependencyGAV.createDefault("com.acme.jsp", "jsp", "1.0.4")));
    Map<Path, List<FileDependency>> moduleDependencyMap =
        Map.of(
            module1Pom, List.of(marsDependency),
            module2Pom, List.of(venusDependency),
            rootPom, List.of(jspDependency));

    this.dependencyMapper = mock(PomFileDependencyMapper.class);
    when(dependencyMapper.build(any(), any())).thenReturn(moduleDependencyMap);

    changedPom1 = mock(ChangedFile.class);
    when(changedPom1.originalFilePath()).thenReturn(module1Pom.toAbsolutePath().toString());
    when(changedPom1.modifiedFile()).thenReturn("changed pom1");

    changedPom2 = mock(ChangedFile.class);
    when(changedPom2.originalFilePath()).thenReturn(module2Pom.toAbsolutePath().toString());
    when(changedPom2.modifiedFile()).thenReturn("changed pom2");

    changedRootPom = mock(ChangedFile.class);
    when(changedRootPom.originalFilePath()).thenReturn(rootPom.toAbsolutePath().toString());
    when(changedRootPom.modifiedFile()).thenReturn("changed pom4");

    this.pomUpdater = mock(PomFileUpdater.class);
    when(pomUpdater.updatePom(eq(module1Pom), any())).thenReturn(Optional.of(changedPom1));
    when(pomUpdater.updatePom(eq(module2Pom), any())).thenReturn(Optional.of(changedPom2));
    when(pomUpdater.updatePom(eq(rootPom), any())).thenReturn(Optional.of(changedRootPom));

    this.irrelevant = mock(ChangedFile.class);
    when(irrelevant.originalFilePath()).thenReturn(irrelevantFile.toAbsolutePath().toString());
    when(irrelevant.modifiedFile()).thenReturn("irrelevant");
    when(irrelevant.weaves()).thenReturn(List.of(Weave.from(5, "not-real-change")));
  }

  @Test
  void it_updates_all_pom_files_correctly_when_no_issues() throws IOException {
    MavenProvider provider = new MavenProvider(dependencyMapper, pomUpdater);
    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir,
            Set.of(irrelevant),
            List.of(marsDependency, venusDependency, jspDependency));

    // no errors, success!
    assertThat(result.erroredFiles()).isEmpty();

    // we've updated all the poms, so we merge this with the pre-existing one change
    assertThat(result.updatedChanges())
        .containsOnly(irrelevant, changedPom1, changedPom2, changedRootPom);

    // we injected all the dependencies, total success!
    assertThat(result.injectedDependencies())
        .containsOnly(marsDependency, venusDependency, jspDependency);
  }

  @Test
  void it_handles_pom_update_unnecessary() throws IOException {
    MavenProvider provider = new MavenProvider(dependencyMapper, pomUpdater);

    // introduce a failure
    when(pomUpdater.updatePom(eq(module2Pom), any())).thenReturn(Optional.empty());

    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir, Set.of(irrelevant), List.of(marsDependency, jspDependency));

    // no errors, success!
    assertThat(result.erroredFiles()).isEmpty();

    // we've updated all but the module2 pom, so we merge this with the pre-existing one change
    assertThat(result.updatedChanges()).containsOnly(irrelevant, changedPom1, changedRootPom);

    // we injected all but one of the deps
    assertThat(result.injectedDependencies()).containsOnly(marsDependency, jspDependency);
  }

  @Test
  void it_handles_pom_update_failure() throws IOException {
    MavenProvider provider = new MavenProvider(dependencyMapper, pomUpdater);

    // introduce a failure
    when(pomUpdater.updatePom(eq(module1Pom), any()))
        .thenThrow(new IOException("failed to update pom"));

    DependencyUpdateResult result =
        provider.updateDependencies(
            projectDir, Set.of(irrelevant), List.of(venusDependency, jspDependency));

    // no errors, success!
    assertThat(result.erroredFiles()).containsOnly(module1Pom);

    // we've updated all but the module1 pom, so we merge this with the pre-existing one change
    assertThat(result.updatedChanges()).containsOnly(irrelevant, changedPom2, changedRootPom);

    // we injected all but one of the deps
    assertThat(result.injectedDependencies()).containsOnly(venusDependency, jspDependency);
  }

  @Test
  void it_builds_correct_dependency_map() throws IOException {
    PomFileDependencyMapper dependencyMapper = new MavenProvider.DefaultPomFileDependencyMapper();
    Map<Path, List<FileDependency>> map =
        dependencyMapper.build(projectDir, List.of(marsDependency, venusDependency, jspDependency));
    assertThat(map).containsOnlyKeys(module1Pom, rootPom);
    assertThat(map.get(module1Pom)).containsOnly(marsDependency, venusDependency);
    assertThat(map.get(rootPom)).containsOnly(jspDependency);
  }

  /**
   * This tests that we can update a pom file that has already been changed by another codemod
   * previously.
   */
  @Test
  void it_can_update_pom_after_existing_changes(final @TempDir Path tmpDir) throws IOException {

    MavenProvider provider = new MavenProvider(dependencyMapper, pomUpdater);

    // we record a change to the module1/pom.xml file, which started as an empty project, but has
    // been
    // changed into the simplePom value
    Path tmpPomFile = tmpDir.resolve("pom.xml");
    ChangedFile alreadyChangedPom1 = mock(ChangedFile.class);
    when(alreadyChangedPom1.originalFilePath()).thenReturn(module1Pom.toAbsolutePath().toString());
    when(alreadyChangedPom1.modifiedFile()).thenReturn(tmpPomFile.toAbsolutePath().toString());
    String initialPomContents = "<?xml version=\"1.0\"?><project></project>";
    Files.write(module1Pom, initialPomContents.getBytes(StandardCharsets.UTF_8));
    Files.write(tmpPomFile, simplePom.getBytes(StandardCharsets.UTF_8));

    // after updating, the old change record should be gone, and the new change record should have
    // the contents of the
    // simplePomAfterChanges. we should also have the old file backup restored
    ChangedFile secondChangePom1 = mock(ChangedFile.class);
    when(secondChangePom1.originalFilePath()).thenReturn(module1Pom.toAbsolutePath().toString());
    Path module1PomAfterSecondChange = Files.createTempFile("pom", "xml");
    Files.write(
        module1PomAfterSecondChange, simplePomAfterChanges.getBytes(StandardCharsets.UTF_8));
    when(secondChangePom1.modifiedFile())
        .thenReturn(module1PomAfterSecondChange.toAbsolutePath().toString());

    when(pomUpdater.updatePom(eq(module1Pom), any())).thenReturn(Optional.of(secondChangePom1));
    when(pomUpdater.updatePom(eq(module2Pom), any())).thenReturn(Optional.empty());
    when(pomUpdater.updatePom(eq(module3Pom), any())).thenReturn(Optional.empty());
    when(pomUpdater.updatePom(eq(rootPom), any())).thenReturn(Optional.empty());
    Set<ChangedFile> changes = Set.of(irrelevant, alreadyChangedPom1);

    DependencyUpdateResult result =
        provider.updateDependencies(projectDir, changes, List.of(marsDependency, venusDependency));

    Set<ChangedFile> updatedChanges = result.updatedChanges();
    assertThat(updatedChanges).containsOnly(irrelevant, secondChangePom1);

    // confirm the file was successfully restored up
    assertThat(Files.readString(module1Pom, StandardCharsets.UTF_8)).isEqualTo(initialPomContents);
  }

  @Test
  void it_updates_poms_correctly() throws IOException {
    PomFileUpdater updater = new MavenProvider.DefaultPomFileUpdater();
    Files.write(module1Pom, simplePom.getBytes(StandardCharsets.UTF_8));
    Optional<ChangedFile> result =
        updater.updatePom(module1Pom, List.of(marsDependency, venusDependency));
    assertThat(result).isPresent();
    assertThat(result.get().originalFilePath()).isEqualTo(module1Pom.toAbsolutePath().toString());
    String modifiedFile = result.get().modifiedFile();
    String updatedPomContents = Files.readString(Path.of(modifiedFile), StandardCharsets.UTF_8);
    assertThat(updatedPomContents).isEqualToIgnoringWhitespace(simplePomAfterChanges);
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
