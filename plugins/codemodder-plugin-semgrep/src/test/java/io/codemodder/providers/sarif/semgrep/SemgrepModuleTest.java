package io.codemodder.providers.sarif.semgrep;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.google.inject.*;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.codemodder.*;
import io.codemodder.codetf.CodeTFReference;
import java.io.IOException;
import java.lang.annotation.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Qualifier;
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

  @Documented
  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.FIELD)
  public @interface FakeScan {
    String value();
  }

  @Singleton
  public static class MyInnerType1 {
    @Inject
    @FakeScan("scan1")
    private RuleSarif sarif;

    public MyInnerType1() {}
  }

  @Singleton
  public static class MyInnerType2 {
    @Inject
    @FakeScan("scan2")
    private RuleSarif sarif;

    public MyInnerType2() {}
  }

  public static class MyComposedType {
    private final MyInnerType1 inner1;
    private final MyInnerType2 inner2;

    @Inject
    public MyComposedType(MyInnerType1 inner1, MyInnerType2 inner2) {
      this.inner1 = inner1;
      this.inner2 = inner2;
    }
  }

  static class RuleSarifImpl implements RuleSarif {

    private String id;

    RuleSarifImpl(String id) {
      this.id = id;
    }

    @Override
    public List<Region> getRegionsFromResultsByRule(Path path) {
      return null;
    }

    @Override
    public List<Result> getResultsByPath(Path path) {
      return null;
    }

    @Override
    public SarifSchema210 rawDocument() {
      return null;
    }

    @Override
    public String getRule() {
      return id;
    }

    @Override
    public String getDriver() {
      return null;
    }
  }

  @Test
  void it_injects_scan() {
    TypeListener listener =
        new TypeListener() {
          @Override
          public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            Class<?> clazz = type.getRawType();
            encounter.register(
                new MembersInjector<>() {
                  @Override
                  public void injectMembers(I instance) {
                    for (Field field : clazz.getDeclaredFields()) {
                      if (field.isAnnotationPresent(FakeScan.class)) {
                        FakeScan annotation = field.getAnnotation(FakeScan.class);
                        try {
                          field.setAccessible(true);
                          RuleSarif sarif = new RuleSarifImpl(annotation.value());
                          field.set(instance, sarif);
                        } catch (IllegalAccessException e) {
                          throw new RuntimeException(e);
                        }
                      }
                    }
                  }
                });
          }
        };

    AbstractModule m =
        new AbstractModule() {
          @Override
          protected void configure() {
            bind(RuleSarif.class).annotatedWith(FakeScan.class).toInstance(new RuleSarifImpl(""));
            binder().bindListener(Matchers.any(), listener);
          }
        };

    Injector injector = Guice.createInjector(m);
    MyComposedType composed = injector.getInstance(MyComposedType.class);
    MyInnerType1 inner1 = injector.getInstance(MyInnerType1.class);
    MyInnerType2 inner2 = injector.getInstance(MyInnerType2.class);
    System.out.println(composed.inner1.sarif.getRule());
    System.out.println(composed.inner2.sarif.getRule());
    System.out.println(composed.inner1.sarif.getRule());
    System.out.println(composed.inner2.sarif.getRule());
    assertThat(composed.inner1, is(inner1));
    assertThat(composed.inner2, is(inner2));
  }
}
