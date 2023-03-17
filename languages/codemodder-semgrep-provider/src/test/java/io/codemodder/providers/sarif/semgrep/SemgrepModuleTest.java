package io.codemodder.providers.sarif.semgrep;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.codemodder.Changer;
import io.codemodder.Codemod;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Tests binding and running while binding. */
final class SemgrepModuleTest {

  @Codemod(
      author = "pixee",
      id = "pixee-test:java/implicit-yaml",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesImplicitYamlPath implements Changer {
    private final RuleSarif ruleSarif;

    @Inject
    UsesImplicitYamlPath(@SemgrepScan(ruleId = "implicit-yaml-path") RuleSarif ruleSarif) {
      this.ruleSarif = ruleSarif;
    }
  }

  @Codemod(
      author = "pixee",
      id = "pixee-test:java/explicit-yaml-test",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesExplicitYamlPath extends SemgrepJavaParserChanger<ObjectCreationExpr> {

    @Inject
    UsesExplicitYamlPath(
        @SemgrepScan(
                pathToYaml = "/other_dir/explicit-yaml-path.yaml",
                ruleId = "explicit-yaml-path")
            RuleSarif ruleSarif) {
      super(ruleSarif, ObjectCreationExpr.class);
    }

    @Override
    public void onSemgrepResultFound(
        final CodemodInvocationContext context,
        final CompilationUnit cu,
        final ObjectCreationExpr node,
        final Result result) {}
  }

  @Codemod(
      author = "pixee",
      id = "pixee-test:java/incorrect-binding-type",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class BindsToIncorrectObject implements Changer {
    @Inject
    BindsToIncorrectObject(
        @SemgrepScan(ruleId = "incorrect-binding-type") HashMap<Object, Object> nonSarifObject) {}
  }

  @Test
  void it_fails_when_injecting_nonsarif_type(@TempDir Path tmpDir) {
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new SemgrepModule(tmpDir, List.of(BindsToIncorrectObject.class)).configure();
        });
  }

  @Test
  void it_works_with_implicit_yaml_path(@TempDir Path tmpDir) throws IOException {
    String javaCode = "class Foo { \n Object a = new Thing(); \n }";
    Path javaFile = Files.createTempFile(tmpDir, "HasThing", ".java");
    Files.write(
        javaFile, javaCode.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);
    SemgrepModule module = new SemgrepModule(tmpDir, List.of(UsesImplicitYamlPath.class));
    Injector injector = Guice.createInjector(module);
    UsesImplicitYamlPath instance = injector.getInstance(UsesImplicitYamlPath.class);

    RuleSarif ruleSarif = instance.ruleSarif;
    assertThat(ruleSarif, is(notNullValue()));
    List<Region> regions = ruleSarif.getRegionsFromResultsByRule(javaFile);
    assertThat(regions.size(), is(1));
  }

  @Test
  void it_works_with_explicit_yaml_path(@TempDir Path tmpDir) throws IOException {
    String javaCode = "class Foo { \n\n  Object a = new Stuff(); \n }";
    Path javaFile = Files.createTempFile(tmpDir, "HasStuff", ".java");
    Files.write(
        javaFile, javaCode.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

    SemgrepModule module = new SemgrepModule(tmpDir, List.of(UsesExplicitYamlPath.class));
    Injector injector = Guice.createInjector(module);
    UsesExplicitYamlPath instance = injector.getInstance(UsesExplicitYamlPath.class);
    RuleSarif ruleSarif = instance.sarif;
    assertThat(ruleSarif, is(notNullValue()));
    assertThat(ruleSarif.getRegionsFromResultsByRule(javaFile).size(), is(1));
  }
}
