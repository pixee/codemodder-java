package io.codemodder.providers.sarif.pmd;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class DefaultPmdRunnerTest {

  private Path projectDir;

  @BeforeEach
  void setup(@TempDir Path tmpDir) throws IOException {
    this.projectDir = tmpDir;
    Path module1JavaDir =
        Files.createDirectories(projectDir.resolve("module1/src/main/java/com/acme/"));
    Path module2JavaDir =
        Files.createDirectories(projectDir.resolve("module2/src/main/java/com/acme/util/"));

    // this file will trip rule: category/java/bestpractices.xml/MissingOverride
    Path fileWithMissingOverride = module1JavaDir.resolve("IsMissingOverride.java");
    Files.writeString(
        fileWithMissingOverride,
        """
                    package com.acme;
                    public class IsMissingOverride implements Runnable {
                        public void run() {
                           System.out.println("i don't have an override on this method!");
                        }
                    }
                    """);

    // this file will trip rule: category/java/bestpractices.xml/OneDeclarationPerLine
    Path fileWithMultipleDeclarationsOnOneLine =
        module2JavaDir.resolve("HasMultipleDeclarationsOnOneLine.java");
    Files.writeString(
        fileWithMultipleDeclarationsOnOneLine,
        """
                    package com.acme.util;
                    public abstract class MultipleDeclarations {
                        public String a, b, c;
                    }
                    """);
  }

  @Test
  void it_runs_and_finds_stuff() throws IOException {
    DefaultPmdRunner runner = new DefaultPmdRunner();
    List<String> ruleIds =
        List.of(
            "category/java/bestpractices.xml/OneDeclarationPerLine",
            "category/java/bestpractices.xml/MissingOverride");
    SarifSchema210 sarif = runner.run(ruleIds, projectDir, Files.list(projectDir).toList());
    assertThat(sarif.getRuns()).hasSize(1);
    Run run = sarif.getRuns().get(0);
    List<Result> results = run.getResults();
    assertThat(results).hasSize(2);
    boolean hasMissingOverride =
        results.stream()
            .anyMatch(
                result ->
                    result.getRuleId().equals("MissingOverride")
                        && result
                            .getLocations()
                            .get(0)
                            .getPhysicalLocation()
                            .getArtifactLocation()
                            .getUri()
                            .endsWith("/IsMissingOverride.java"));

    assertThat(hasMissingOverride).isTrue();

    boolean hasOneDeclarationPerLine =
        results.stream()
            .anyMatch(
                result ->
                    result.getRuleId().equals("OneDeclarationPerLine")
                        && result
                            .getLocations()
                            .get(0)
                            .getPhysicalLocation()
                            .getArtifactLocation()
                            .getUri()
                            .endsWith("/HasMultipleDeclarationsOnOneLine.java"));
    assertThat(hasOneDeclarationPerLine).isTrue();
  }
}
