package io.codemodder.plugins.jpms;

import static org.assertj.core.api.Assertions.assertThat;

import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class JpmsProviderTest {

  private Path tmpDir;
  private Path projectDir;
  private Path javaFileWereChanging;
  private Path moduleInfoJavaFile;
  private List<DependencyGAV> dependencies;
  private JpmsProvider provider;

  private static final String comAcmeModule =
      """
            module com.acme {
              exports com.acme;
            }
            """;

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

    Files.writeString(moduleInfoJavaFile, comAcmeModule);

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

  @Test
  void it_doesnt_inject_when_no_dependencies() throws IOException {
    dependencies = List.of();
    verifyNoChangesMade();
  }

  @Test
  void it_doesnt_inject_when_no_module_insertable_dependencies() throws IOException {
    dependencies = List.of(DependencyGAV.OWASP_XSS_JAVA_ENCODER);
    verifyNoChangesMade();
  }

  @Test
  void it_fails_gracefully_when_no_module() throws IOException {
    Files.delete(moduleInfoJavaFile);
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
    Files.writeString(moduleInfoJavaFile, comAcmeModule);

    verifySuccessfulInjection();
  }

  @Test
  void it_skips_when_present() throws IOException {
    dependencies = List.of(DependencyGAV.JAVA_SECURITY_TOOLKIT);
    String comAcmeModuleAlreadyHasSecurityToolkit =
        """
            module com.acme {
              exports com.acme;
                requires io.github.pixee.security; // weird indentation or comments shouldn't throw it off
            }
            """;
    Files.writeString(moduleInfoJavaFile, comAcmeModuleAlreadyHasSecurityToolkit);
    DependencyUpdateResult result =
        provider.updateDependencies(projectDir, javaFileWereChanging, dependencies);
    assertThat(result.skippedPackages()).containsOnly(DependencyGAV.JAVA_SECURITY_TOOLKIT);
    assertThat(result.injectedPackages()).isEmpty();
    assertThat(result.erroredFiles()).isEmpty();
    assertThat(result.packageChanges()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("indentTestCases")
  void it_indents_correctly(final String in, final String expectedOut) throws IOException {
    Files.writeString(moduleInfoJavaFile, in);
    provider.updateDependencies(projectDir, javaFileWereChanging, dependencies);
    String contents = Files.readString(moduleInfoJavaFile);

    // should have detected that there's a weird 5 space indent
    assertThat(contents).isEqualTo(expectedOut);
  }

  public static Stream<Arguments> indentTestCases() {
    return Stream.of(
        // this one uses a non-standard amount of spaces: 5
        Arguments.of(
            """
            module com.acme {
                 exports com.acme;
            }
            """,
            """
                    module com.acme {
                         exports com.acme;
                         requires io.github.pixee.security;
                    }
                    """),
        // if you don't use a space, we won't either
        Arguments.of(
            """
            module com.acme {
            exports com.acme;
            }
            """,
            """
                    module com.acme {
                    exports com.acme;
                    requires io.github.pixee.security;
                    }
                    """),
        // with nothing, we'll fall back to 2
        Arguments.of(
            """
            module com.acme {

            }
            """,
            """
                    module com.acme {
                      requires io.github.pixee.security;

                    }
                    """));
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
        .isEqualTo(
            """
                        module com.acme {
                          exports com.acme;
                          requires io.github.pixee.security;
                        }
                        """);
  }
}
