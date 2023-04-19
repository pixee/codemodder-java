package io.codemodder.providers.sarif.semgrep;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.codemodder.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class SemgrepJavaParserChangerTest {

  private static final String FINDS_THAT_SEMGREP_YAML =
      "rules:\n  - id: inline-semgrep\n    pattern: new Stuff()\n    languages:\n      - java\n    message: Semgrep found a match\n    severity: WARNING\n";

  @Codemod(
      author = "pixee",
      id = "pixee-test:java/finds-stuff",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class ExplicitPathCodemod extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    @Inject
    ExplicitPathCodemod(
        @SemgrepScan(
                pathToYaml = "/other_dir/explicit-yaml-path.yaml",
                ruleId = "explicit-yaml-path")
            RuleSarif ruleSarif) {
      super(ruleSarif, ObjectCreationExpr.class);
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final ObjectCreationExpr node,
        final Result result) {
      return true;
    }
  }

  @Test
  void it_gives_sarif_off_explicit_yaml_path(@TempDir Path tmpDir) throws IOException {
    String javaCode = "class Foo { \n\n  Object a = new Stuff(); \n }";
    Path javaFile = writeJavaFile(tmpDir, javaCode);

    SemgrepModule module = new SemgrepModule(tmpDir, List.of(ExplicitPathCodemod.class));
    Injector injector = Guice.createInjector(module);
    ExplicitPathCodemod instance = injector.getInstance(ExplicitPathCodemod.class);
    RuleSarif ruleSarif = instance.sarif;
    assertThat(ruleSarif, is(notNullValue()));
    assertThat(ruleSarif.getRegionsFromResultsByRule(javaFile).size(), is(1));
  }

  @Test
  void it_gives_sarif_off_inline_yaml(@TempDir Path tmpDir) throws IOException {
    String javaCode = "class Foo { \n\n  Object a = new Stuff();\n  Object b = new That();\n }";
    Path javaFile = writeJavaFile(tmpDir, javaCode);

    SemgrepModule module = new SemgrepModule(tmpDir, List.of(UsesInlineSemgrepCodemod.class));
    Injector injector = Guice.createInjector(module);
    UsesInlineSemgrepCodemod instance = injector.getInstance(UsesInlineSemgrepCodemod.class);
    RuleSarif ruleSarif = instance.sarif;
    assertThat(ruleSarif, is(notNullValue()));
    assertThat(ruleSarif.getRegionsFromResultsByRule(javaFile).size(), is(1));
  }

  @Test
  void it_fails_when_both_used(@TempDir Path tmpDir) {
    SemgrepModule module = new SemgrepModule(tmpDir, List.of(InvalidUsesBothYamlStrategies.class));
    assertThrows(CreationException.class, () -> Guice.createInjector(module));
  }

  private Path writeJavaFile(final Path tmpDir, final String javaCode) throws IOException {
    Path javaFile = Files.createTempFile(tmpDir, "HasStuff", ".java");
    Files.write(
        javaFile, javaCode.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
    return javaFile;
  }

  @Codemod(
      author = "pixee",
      id = "pixee-test:java/uses-inline-semgrep",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesInlineSemgrepCodemod extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    @Inject
    UsesInlineSemgrepCodemod(
        @SemgrepScan(yaml = FINDS_THAT_SEMGREP_YAML, ruleId = "inline-semgrep")
            RuleSarif ruleSarif) {
      super(ruleSarif, ObjectCreationExpr.class);
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final ObjectCreationExpr node,
        final Result result) {
      return true;
    }
  }

  @Codemod(
      author = "pixee",
      id = "pixee-test:java/both-yaml",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class InvalidUsesBothYamlStrategies
      extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    @Inject
    InvalidUsesBothYamlStrategies(
        @SemgrepScan(
                yaml = FINDS_THAT_SEMGREP_YAML,
                pathToYaml = "/other_dir/explicit-yaml-path.yaml",
                ruleId = "explicit-yaml-path")
            RuleSarif ruleSarif) {
      super(ruleSarif, ObjectCreationExpr.class);
    }

    @Override
    public boolean onResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final ObjectCreationExpr node,
        final Result result) {
      return true;
    }
  }
}
