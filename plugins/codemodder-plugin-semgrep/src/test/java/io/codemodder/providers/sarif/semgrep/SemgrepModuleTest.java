package io.codemodder.providers.sarif.semgrep;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
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
import java.util.Map;
import java.util.Objects;
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
      importance = Importance.LOW,
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
      id = "pixee-test:java/explicit-yaml-test",
      importance = Importance.LOW,
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
          SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION,
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
      importance = Importance.LOW,
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
          SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION,
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
      importance = Importance.LOW,
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
          SourceCodeRegionExtractor.FROM_SARIF_FIRST_LOCATION,
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
    SemgrepModule module = createModule(tmpDir, List.of(UsesImplicitYamlPath.class));
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

    SemgrepModule module = createModule(tmpDir, List.of(codemod));
    Injector injector = Guice.createInjector(module);
    SarifPluginJavaParserChanger<ObjectCreationExpr> instance =
        (SarifPluginJavaParserChanger<ObjectCreationExpr>) injector.getInstance(codemod);
    RuleSarif ruleSarif = instance.sarif;
    assertThat(ruleSarif, is(notNullValue()));
    assertThat(ruleSarif.getRegionsFromResultsByRule(javaFile).size(), is(1));
  }

  @Test
  void it_detects_rule_ids() {
    String id =
        DefaultSemgrepRuleFactory.detectSingleRuleFromYaml(
            "rules:\n  - id: foo\n    pattern: bar\n");
    assertThat(id, is("foo"));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            DefaultSemgrepRuleFactory.detectSingleRuleFromYaml(
                "rules:\n  - id: foo\n  - id: bar\n    pattern: baz\n"));
    assertThrows(
        IllegalArgumentException.class,
        () -> DefaultSemgrepRuleFactory.detectSingleRuleFromYaml("rules:\n  - pattern: baz\n"));
  }

  private SemgrepModule createModule(
      final Path dir, final List<Class<? extends CodeChanger>> codemodTypes) throws IOException {
    return new SemgrepModule(dir, List.of("**"), List.of(), codemodTypes);
  }

  static Stream<Arguments> codemodsThatLookForNewStuffInstances() {
    return Stream.of(
        Arguments.of(UsesExplicitYamlPath.class),
        Arguments.of(MissingYamlPropertiesPath.class),
        Arguments.of(UsesImplicitRule.class));
  }

  @Test
  void it_works_with_offline_semgrep(@TempDir Path tmpDir) throws IOException {
    Path directory =
        Files.createDirectories(tmpDir.resolve("acme-webapp/core/src/main/java/com/acme/core/"));
    Path javaFile = directory.resolve("RDSUtil.java");
    Files.createFile(javaFile);
    Files.writeString(javaFile, vulnCode, StandardOpenOption.TRUNCATE_EXISTING);
    Map<String, List<RuleSarif>> map =
        SarifParser.create()
            .parseIntoMap(
                List.of(Path.of("src/test/resources/semgrep_with_reflection_injection.sarif")),
                tmpDir);
    SemgrepModule module =
        new SemgrepModule(
            tmpDir,
            List.of("**"),
            List.of(),
            List.of(UsesOfflineSemgrepCodemod.class),
            map.entrySet().iterator().next().getValue(),
            new DefaultSemgrepRuleFactory());
    Injector injector = Guice.createInjector(module);
    UsesOfflineSemgrepCodemod instance = injector.getInstance(UsesOfflineSemgrepCodemod.class);

    RuleSarif ruleSarif = instance.ruleSarif;
    assertThat(ruleSarif, is(notNullValue()));
    List<Region> regions = ruleSarif.getRegionsFromResultsByRule(javaFile);
    assertThat(regions.size(), is(1));

    Region region = regions.get(0);
    assertThat(region.getStartLine(), equalTo(91));
    assertThat(region.getStartColumn(), equalTo(7));
    assertThat(
        ruleSarif.getRule(),
        equalTo("java.lang.security.audit.unsafe-reflection.unsafe-reflection"));
    assertThat(ruleSarif.getDriver(), equalTo("semgrep"));
  }

  @Codemod(
      id = "pixee-test:java/offline-semgrep",
      importance = Importance.LOW,
      reviewGuidance = ReviewGuidance.MERGE_AFTER_CURSORY_REVIEW)
  static class UsesOfflineSemgrepCodemod implements CodeChanger {

    private final RuleSarif ruleSarif;

    @Inject
    UsesOfflineSemgrepCodemod(
        @ProvidedSemgrepScan(
                ruleId = "java.lang.security.audit.unsafe-reflection.unsafe-reflection")
            final RuleSarif ruleSarif) {
      this.ruleSarif = Objects.requireNonNull(ruleSarif);
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

  private static final String vulnCode =
      """
          package com.acme.core;

          import com.amazonaws.services.secretsmanager.AWSSecretsManager;
          import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
          import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
          import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
          import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
          import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
          import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;
          import java.sql.Connection;
          import java.sql.DriverManager;
          import java.sql.SQLException;
          import org.apache.logging.log4j.LogManager;
          import org.apache.logging.log4j.Logger;

          /** Default way to connect to RDS instance. */
          public final class RDSUtil {

            private static final int PORT = 6609;

            /**
             * This fake utility connects to a database!
             * But it's very real looking!
             */
            public static Connection connectToDatabase() throws SQLException {
              AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.standard().build();
              String host = getHost(ssm);
              String name = getName(ssm);
              String user = getUser(ssm);
              String password = getPassword();
              String dbUrl = getUrl(host, name);
              LOG.warn("Connecting to {}", dbUrl);
              return DriverManager.getConnection(dbUrl, user, password);
            }

            public static String getPassword() {
              String passwordSecretIdArn = System.getenv("ACME_PASSWORD_ID");
              AWSSecretsManager secretsManager = AWSSecretsManagerClientBuilder.standard().build();
              GetSecretValueRequest secretRequest =
                  new GetSecretValueRequest().withSecretId(passwordSecretIdArn);
              GetSecretValueResult secretValue = secretsManager.getSecretValue(secretRequest);
              return secretValue.getSecretString();
            }

            public static String getHost() {
              AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.standard().build();
              return getHost(ssm);
            }

            private static String getHost(final AWSSimpleSystemsManagement ssm) {
              String hostParameterName = System.getenv("ACME_DB_HOST");
              return ssm.getParameter(new GetParameterRequest().withName(hostParameterName))
                  .getParameter()
                  .getValue();
            }

            public static String getName() {
              AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.standard().build();
              return getName(ssm);
            }

            private static String getName(final AWSSimpleSystemsManagement ssm) {
              String nameParameterName = System.getenv("ACME_DB_NAME");
              return ssm.getParameter(new GetParameterRequest().withName(nameParameterName))
                  .getParameter()
                  .getValue();
            }

            public static String getUser() {
              AWSSimpleSystemsManagement ssm = AWSSimpleSystemsManagementClientBuilder.standard().build();
              return getUser(ssm);
            }

            private static String getUser(final AWSSimpleSystemsManagement ssm) {
              String userParameterName = System.getenv("ACME_DB_USER");
              return ssm.getParameter(new GetParameterRequest().withName(userParameterName))
                  .getParameter()
                  .getValue();
            }

            public static String getUrl() {
              return getUrl(getHost(), getName());
            }

            public static String getUrl(final String host, final String name) {
              return "jdbc:mariadb://" + host + ":" + PORT + "/" + name;
            }

            static {
              try {
                Class.forName(getDriverName());
              } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Couldn't load mariadb driver", e);
              }
            }

            public static String getDriverName() {
              return "org.mariadb.jdbc.Driver";
            }

            private static final Logger LOG = LogManager.getLogger(RDSUtil.class);
          }
          """;
}
