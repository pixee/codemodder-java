package io.codemodder.plugins.jpms;

import static org.assertj.core.api.Assertions.assertThat;

import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class JpmsProviderTest {

  private Path tmpDir;
  private Path projectDir;
  private Path javaFileWereChanging;
  private Path moduleInfoJavaFile;
  private List<DependencyGAV> dependencies;
  private JpmsProvider provider;

  @BeforeEach
  void setup(@TempDir Path tmpDir) throws IOException {
    this.tmpDir = tmpDir;
    this.projectDir = tmpDir.resolve("my-project");
    Files.createDirectory(projectDir);
    this.javaFileWereChanging = projectDir.resolve("module1/src/main/java/com/acme/Acme.java");
    this.moduleInfoJavaFile = projectDir.resolve("module1/src/main/java/module-info.java");

    Files.createDirectories(javaFileWereChanging.getParent());
    Files.createFile(javaFileWereChanging);
    Files.writeString(javaFileWereChanging, "package com.acme;\n\npublic class Acme {}");

    Files.createFile(moduleInfoJavaFile);
    Files.writeString(moduleInfoJavaFile, "module com.acme { }");

    // the java security toolkit has a module name, but not the OWASP encoder
    this.dependencies =
        List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT, DependencyGAV.OWASP_XSS_JAVA_ENCODER);
    this.provider = new JpmsProvider();
  }

  @Test
  void it_injects_source_root_correctly() throws IOException {
    verifySuccessfulInjection();
  }

  @Test
  void it_doesnt_escape_project_root() throws IOException {
    // by putting the module-info.java below the project dir, we should not find it
    Files.move(moduleInfoJavaFile, tmpDir.resolve("module-info.java"));
    verifyNoChangesMade();
  }

  private void verifyNoChangesMade() throws IOException {
    DependencyUpdateResult result =
        provider.updateDependencies(projectDir, javaFileWereChanging, dependencies);
    assertThat(result.packageChanges()).isEmpty();
    assertThat(result.injectedPackages()).isEmpty();
    assertThat(result.skippedPackages()).isEmpty();
    assertThat(result.erroredFiles()).isEmpty();
  }

  @Test
  void it_doesnt_inject_when_package_dir_is_not_for_us() throws IOException {
    Files.delete(moduleInfoJavaFile);
    // because the package name doesn't match the module name, we shouldn't inject
    Path packageDir = projectDir.resolve("module1/src/main/java/com.skadoodle");
    Files.createDirectories(packageDir);
    moduleInfoJavaFile = packageDir.resolve("module-info.java");
    Files.writeString(moduleInfoJavaFile, "module com.skadoodle { }");

    verifyNoChangesMade();
  }

  @Test
  void it_injects_into_source_then_package_dir_correctly() throws IOException {
    Files.delete(moduleInfoJavaFile);
    Path packageDir = projectDir.resolve("module1/src/main/java/com.acme");
    Files.createDirectories(packageDir);
    moduleInfoJavaFile = packageDir.resolve("module-info.java");
    Files.writeString(moduleInfoJavaFile, "module com.acme { }");

    verifySuccessfulInjection();
  }

  private void verifySuccessfulInjection() throws IOException {
    DependencyUpdateResult result =
        provider.updateDependencies(projectDir, javaFileWereChanging, dependencies);
    List<DependencyGAV> injectedPackages = result.injectedPackages();
    List<DependencyGAV> skippedPackages = result.skippedPackages();

    // we should only have the toolkit, because the OWASP encoder doesn't have a module name
    assertThat(injectedPackages).containsOnly(DependencyGAV.JAVA_SECURITY_TOOLKIT);
    assertThat(skippedPackages).containsOnly(DependencyGAV.OWASP_XSS_JAVA_ENCODER);
    assertThat(result.erroredFiles()).isEmpty();

    // the module-info.java file should have been updated
    String contents = Files.readString(moduleInfoJavaFile);
    assertThat(contents)
        .isEqualToIgnoringWhitespace("module com.acme { requires io.github.pixee.security; }");
  }
}
