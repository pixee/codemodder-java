package io.codemodder.plugins.maven;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import io.codemodder.DependencyDescriptor;
import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DefaultPOMDependencyUpdaterTest {

  private ArtifactInjectionPositionFinder positionFinder;
  private CodeTFGenerator codeTFGenerator;
  private DependencyDescriptor dependencyDescriptor;
  private PomFileFinder pomFileFinder;
  private MavenProvider.PomModifier pomModifier;
  private Path projectDir;
  private Path pomPath;
  private DefaultPOMDependencyUpdater updater;

  @BeforeEach
  void setup(@TempDir Path tempDir) throws IOException {
    positionFinder = mock(ArtifactInjectionPositionFinder.class);
    dependencyDescriptor = DependencyDescriptor.createMarkdownDescriptor();
    pomFileFinder = mock(PomFileFinder.class);
    pomModifier = mock(MavenProvider.PomModifier.class);
    this.projectDir = tempDir;
    build();
  }

  void build() throws IOException {
    pomPath = projectDir.resolve("pom.xml");

    if (!Files.exists(pomPath)) {
      Files.createFile(pomPath);
    }
    Files.writeString(
        pomPath,
        """
                <project>
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>io.codemodder</groupId>
                    <artifactId>test</artifactId>
                    <version>1.0.0</version>
                    <dependencies>

                    </dependencies>
                </project>
                """);

    when(pomFileFinder.findForFile(any(), any())).thenReturn(Optional.ofNullable(pomPath));
    codeTFGenerator = new CodeTFGenerator(positionFinder, dependencyDescriptor);
    when(positionFinder.find(any(), any())).thenReturn(7);
    updater = new DefaultPOMDependencyUpdater(codeTFGenerator, pomFileFinder, pomModifier);
  }

  @Test
  void it_works() {
    DependencyUpdateResult result =
        updater.execute(projectDir, pomPath, List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER));
    assertThat(result.erroredFiles()).isEmpty();
    assertThat(result.injectedPackages()).containsExactly(DependencyGAV.OWASP_XSS_JAVA_ENCODER);
    assertThat(result.skippedPackages()).isEmpty();
    assertThat(result.packageChanges()).hasSize(1);
  }

  @Test
  void it_doesnt_propagate_update_exceptions_when_no_pom_found() throws IOException {
    when(pomFileFinder.findForFile(any(), any()))
        .thenThrow(new IOException("blows up during finding"));
    DependencyUpdateResult result =
        updater.execute(projectDir, pomPath, List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER));
    assertThat(result.erroredFiles()).isEmpty();
    assertThat(result.injectedPackages()).isEmpty();
    assertThat(result.skippedPackages()).isEmpty();
    assertThat(result.packageChanges()).isEmpty();
  }

  @Test
  void it_doesnt_propagate_update_exceptions_when_pom_io_error() throws IOException {
    build();
    doThrow(new IOException("blows up during modification")).when(pomModifier).modify(any(), any());
    DependencyUpdateResult result =
        updater.execute(projectDir, pomPath, List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER));
    assertThat(result.erroredFiles()).hasSize(1);
    assertThat(result.injectedPackages()).isEmpty();
    assertThat(result.skippedPackages()).isEmpty();
    assertThat(result.packageChanges()).isEmpty();
  }
}
