package io.codemodder.providers.sarif.semgrep;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests binding and running while binding. */
final class SemgrepModuleTest {

  @Codemod(
      author = "pixee",
      id = "pixee-test:java/implicit-yaml",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesImplicitYamlPath implements CodeChanger {
    private final RuleSarif ruleSarif;

    @Inject
    UsesImplicitYamlPath(@SemgrepScan(ruleId = "implicit-yaml-path") RuleSarif ruleSarif) {
      this.ruleSarif = ruleSarif;
    }

    @Override
    public String getSummary() {
      return null;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public List<CodeTFReference> getReferences() {
      return null;
    }

    @Override
    public String getIndividualChangeDescription(Path filePath, CodemodChange change) {
      return null;
    }
  }

  @Codemod(
      author = "pixee",
      id = "pixee-test:java/explicit-yaml-test",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesExplicitYamlPath extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    @Inject
    UsesExplicitYamlPath(
        @SemgrepScan(
                pathToYaml = "/other_dir/explicit-yaml-path.yaml",
                ruleId = "explicit-yaml-path")
            RuleSarif ruleSarif) {
      super(
          ruleSarif,
          ObjectCreationExpr.class,
          RegionExtractor.FROM_FIRST_LOCATION,
          RegionNodeMatcher.EXACT_MATCH,
          new UselessReporter());
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
      id = "pixee-test:java/missing-properties-test",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class MissingYamlPropertiesPath extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    private static final String YAML_MISSING_STUFF =
        "rules:\n" + "  - id: explicit-yaml-path\n" + "    pattern: new Stuff()\n";

    @Inject
    MissingYamlPropertiesPath(
        @SemgrepScan(yaml = YAML_MISSING_STUFF, ruleId = "explicit-yaml-path")
            RuleSarif ruleSarif) {
      super(
          ruleSarif,
          ObjectCreationExpr.class,
          RegionExtractor.FROM_FIRST_LOCATION,
          RegionNodeMatcher.EXACT_MATCH,
          new UselessReporter());
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
      id = "pixee-test:java/uses-implicit-rule",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesImplicitRule extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    private static final String YAML_MISSING_STUFF =
        "rules:\n" + "  - id: explicit-yaml-path\n" + "    pattern: new Stuff()\n";

    @Inject
    UsesImplicitRule(@SemgrepScan(yaml = YAML_MISSING_STUFF) RuleSarif ruleSarif) {
      super(
          ruleSarif,
          ObjectCreationExpr.class,
          RegionExtractor.FROM_FIRST_LOCATION,
          RegionNodeMatcher.EXACT_MATCH,
          new UselessReporter());
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
      id = "pixee-test:java/uses-implicit-rule",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesImplicitButHasMultipleRules
      extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    private static final String YAML_MISSING_STUFF =
        "rules:\n"
            + "  - id: explicit-yaml-path\n"
            + "    pattern: new Stuff()\n"
            + "  - id: explicit-yaml-path-also\n"
            + "    pattern: new Bar()\n";

    @Inject
    UsesImplicitButHasMultipleRules(@SemgrepScan(yaml = YAML_MISSING_STUFF) RuleSarif ruleSarif) {
      super(
          ruleSarif,
          ObjectCreationExpr.class,
          RegionExtractor.FROM_FIRST_LOCATION,
          RegionNodeMatcher.EXACT_MATCH,
          new UselessReporter());
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
      id = "pixee-test:java/incorrect-binding-type",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class BindsToIncorrectObject implements CodeChanger {
    @Inject
    BindsToIncorrectObject(
        @SemgrepScan(ruleId = "incorrect-binding-type") HashMap<Object, Object> nonSarifObject) {}

    @Override
    public String getSummary() {
      return null;
    }

    @Override
    public String getDescription() {
      return null;
    }

    @Override
    public List<CodeTFReference> getReferences() {
      return null;
    }

    @Override
    public String getIndividualChangeDescription(Path filePath, CodemodChange change) {
      return null;
    }
  }

  @Test
  void it_fails_when_injecting_nonsarif_type(@TempDir Path tmpDir) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SemgrepModule(tmpDir, List.of(BindsToIncorrectObject.class)).configure());
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

  @ParameterizedTest
  @MethodSource("codemodsThatLookForNewStuffInstances")
  void it_works_with_explicit_yaml_path(
      final Class<? extends CodeChanger> codemod, @TempDir Path tmpDir) throws IOException {
    String javaCode = "class Foo { \n\n  Object a = new Stuff(); \n }";
    Path javaFile = Files.createTempFile(tmpDir, "HasStuff", ".java");
    Files.write(
        javaFile, javaCode.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

    SemgrepModule module = new SemgrepModule(tmpDir, List.of(codemod));
    Injector injector = Guice.createInjector(module);
    SarifPluginJavaParserChanger<ObjectCreationExpr> instance =
        (SarifPluginJavaParserChanger<ObjectCreationExpr>) injector.getInstance(codemod);
    RuleSarif ruleSarif = instance.sarif;
    assertThat(ruleSarif, is(notNullValue()));
    assertThat(ruleSarif.getRegionsFromResultsByRule(javaFile).size(), is(1));
  }

  @Test
  void it_fails_when_implicit_rule_but_multiple_specified(@TempDir Path tmpDir) throws IOException {
    String javaCode = "class Foo { \n\n  Object a = new Stuff(); \n }";
    Path javaFile = Files.createTempFile(tmpDir, "HasStuff", ".java");
    Files.write(
        javaFile, javaCode.getBytes(StandardCharsets.UTF_8), StandardOpenOption.TRUNCATE_EXISTING);

    SemgrepModule module =
        new SemgrepModule(tmpDir, List.of(UsesImplicitButHasMultipleRules.class));
    assertThrows(CreationException.class, () -> Guice.createInjector(module));
  }

  @Test
  void it_detects_rule_ids(final @TempDir Path tmpDir) throws IOException {
    SemgrepModule module = new SemgrepModule(tmpDir, List.of(UsesImplicitYamlPath.class));

    String id = module.detectSingleRuleFromYaml("rules:\n  - id: foo\n    pattern: bar\n");
    assertThat(id, is("foo"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            module.detectSingleRuleFromYaml(
                "rules:\n  - id: foo\n  - id: bar\n    pattern: baz\n"));
    assertThrows(
        IllegalArgumentException.class,
        () -> module.detectSingleRuleFromYaml("rules:\n  - pattern: baz\n"));
  }

  static Stream<Arguments> codemodsThatLookForNewStuffInstances() {
    return Stream.of(
        Arguments.of(UsesExplicitYamlPath.class),
        Arguments.of(MissingYamlPropertiesPath.class),
        Arguments.of(UsesImplicitRule.class));
  }
}
