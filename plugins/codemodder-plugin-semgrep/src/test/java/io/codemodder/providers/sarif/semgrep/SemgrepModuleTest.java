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
import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests binding and running while binding.
 *
 * <p>It's worth noting that to test the failing cases testing invalid codemod definitions, we have
 * to move the invalid codemods to their own package. This is because we do package-based classpath
 * scanning to find codemods, and if we find one that is invalid, it breaks everything.
 */
final class SemgrepModuleTest {

  @Codemod(
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
    public Optional<String> getSourceControlUrl() {
      return Optional.empty();
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
          CodemodReporterStrategy.empty());
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
      id = "pixee-test:java/missing-properties-test",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class MissingYamlPropertiesPath extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    private static final String YAML_MISSING_STUFF =
        """
            rules:
              - id: explicit-yaml-path
                pattern: new Stuff()
            """;

    @Inject
    MissingYamlPropertiesPath(
        @SemgrepScan(yaml = YAML_MISSING_STUFF, ruleId = "explicit-yaml-path")
            RuleSarif ruleSarif) {
      super(
          ruleSarif,
          ObjectCreationExpr.class,
          RegionExtractor.FROM_FIRST_LOCATION,
          RegionNodeMatcher.EXACT_MATCH,
          CodemodReporterStrategy.empty());
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
      id = "pixee-test:java/uses-implicit-rule",
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesImplicitRule extends SarifPluginJavaParserChanger<ObjectCreationExpr> {

    private static final String YAML_MISSING_STUFF =
        """
            rules:
              - id: explicit-yaml-path
                pattern: new Stuff()
            """;

    @Inject
    UsesImplicitRule(@SemgrepScan(yaml = YAML_MISSING_STUFF) RuleSarif ruleSarif) {
      super(
          ruleSarif,
          ObjectCreationExpr.class,
          RegionExtractor.FROM_FIRST_LOCATION,
          RegionNodeMatcher.EXACT_MATCH,
          CodemodReporterStrategy.empty());
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
  void it_works_with_implicit_yaml_path(@TempDir Path tmpDir) throws IOException {
    String javaCode = "class Foo { \n Object a = new Thing(); \n }";
    Path javaFile = Files.createTempFile(tmpDir, "HasThing", ".java");
    Files.writeString(javaFile, javaCode, StandardOpenOption.TRUNCATE_EXISTING);
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
    Files.writeString(javaFile, javaCode, StandardOpenOption.TRUNCATE_EXISTING);

    SemgrepModule module = new SemgrepModule(tmpDir, List.of(codemod));
    Injector injector = Guice.createInjector(module);
    SarifPluginJavaParserChanger<ObjectCreationExpr> instance =
        (SarifPluginJavaParserChanger<ObjectCreationExpr>) injector.getInstance(codemod);
    RuleSarif ruleSarif = instance.sarif;
    assertThat(ruleSarif, is(notNullValue()));
    assertThat(ruleSarif.getRegionsFromResultsByRule(javaFile).size(), is(1));
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
