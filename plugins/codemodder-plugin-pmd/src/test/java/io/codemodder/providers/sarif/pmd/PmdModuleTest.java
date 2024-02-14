package io.codemodder.providers.sarif.pmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.codemodder.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class PmdModuleTest {

  @Test
  void it_loads_codemods_and_rule_sarif_bindings_work(@TempDir final Path tmpDir)
      throws IOException {
    Path javaSourceDir = Files.createDirectories(tmpDir.resolve("src/main/java"));
    Path javaFile = javaSourceDir.resolve("MultipleDeclarations.java");
    Files.writeString(
        javaFile,
        """
                    package com.acme.util;
                    public abstract class MultipleDeclarations {
                        public String a, b, c;
                    }
                    """);

    PmdModule module = new PmdModule(tmpDir, List.of(javaFile), List.of(UsesPmdCodemod.class));
    Injector injector = Guice.createInjector(module);
    UsesPmdCodemod codemod = injector.getInstance(UsesPmdCodemod.class);
    RuleSarif ruleSarif = codemod.ruleSarif;
    assertThat(ruleSarif, is(notNullValue()));
    List<Region> regions = ruleSarif.getRegionsFromResultsByRule(javaFile);
    assertThat(regions.size(), is(1));
  }

  @Codemod(
      id = "pmd-test:java/my-pmd-codemod",
      importance = Importance.HIGH,
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesPmdCodemod extends SarifPluginJavaParserChanger<VariableDeclarator> {
    private final RuleSarif ruleSarif;

    @Inject
    UsesPmdCodemod(
        @PmdScan(ruleId = "category/java/bestpractices.xml/OneDeclarationPerLine")
            RuleSarif ruleSarif) {
      super(ruleSarif, VariableDeclarator.class, CodemodReporterStrategy.empty());
      this.ruleSarif = ruleSarif;
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final VariableDeclarator node,
        Result result) {
      return false;
    }
  }
}
