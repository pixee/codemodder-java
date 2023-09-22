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

  @BeforeEach
  void setup(@TempDir Path tmpDir) throws IOException {
    this.tmpDir = tmpDir;
    this.projectDir = tmpDir.resolve("my-project");
    Files.createDirectory(projectDir);
    this.javaFileWereChanging = projectDir.resolve("module1/src/main/java/com/acme/Acme.java");
    this.moduleInfoJavaFile = projectDir.resolve("module1/src/main/java/module-info.java");

    Files.createFile(javaFileWereChanging);
    Files.writeString(javaFileWereChanging, "package com.acme;\n\npublic class Acme {}");

    Files.createFile(moduleInfoJavaFile);
    Files.writeString(javaFileWereChanging, "module com.acme { }");
  }

  @Test
  void it_injects_correctly() throws IOException {
    // the java security toolkit has a module name, but not the OWASP encoder
    List<DependencyGAV> dependencies =
        List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT, DependencyGAV.OWASP_XSS_JAVA_ENCODER);
    JpmsProvider provider = new JpmsProvider();

    DependencyUpdateResult result =
        provider.updateDependencies(projectDir, javaFileWereChanging, dependencies);
    List<DependencyGAV> injectedPackages = result.injectedPackages();
    List<DependencyGAV> skippedPackages = result.skippedPackages();

    // we should only have the toolkit, because the OWASP encoder doesn't have a module name
    assertThat(injectedPackages).containsOnly(DependencyGAV.JAVA_SECURITY_TOOLKIT);
    assertThat(skippedPackages).containsOnly(DependencyGAV.OWASP_XSS_JAVA_ENCODER);
    assertThat(result.erroredFiles()).isEmpty();

    String contents = Files.readString(moduleInfoJavaFile);
    assertThat(contents)
        .isEqualToIgnoringWhitespace("module acme.acme { require io.github.pixee.security; }");
  }

  @Test
  void it_doesnt_escape_project_root() {}
}
