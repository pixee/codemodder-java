package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import com.google.inject.AbstractModule;
import io.codemodder.Changer;
import io.codemodder.RuleSarif;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for binding Semgrep-related things. */
final class SemgrepModule extends AbstractModule {

  private final List<Class<? extends Changer>> codemodTypes;
  private final Path codeDirectory;

  SemgrepModule(final Path codeDirectory, final List<Class<? extends Changer>> codemodTypes) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
  }

  @Override
  protected void configure() {

    /*
     * This sections holds some fair bit of the creation of the "magic" codemod authors get to run. Guice isn't very good
     * about injecting a dependency which depends on an annotation's property, so we have to do some of the leg work
     * to do that binding ourselves.
     */
    List<String> yamlClasspathPathsToRun = new ArrayList<>();
    List<SemgrepScan> toBind = new ArrayList<>();

    for (Class<? extends Changer> codemodType : codemodTypes) {
      // find all constructors that are marked with @Inject
      Constructor<?>[] constructors = codemodType.getDeclaredConstructors();
      List<Constructor<?>> injectableConstructors =
          Arrays.stream(constructors)
              .filter(constructor -> constructor.getAnnotation(javax.inject.Inject.class) != null)
              .collect(Collectors.toUnmodifiableList());

      // find all @SemgrepScan annotations in their parameters and batch them up for running
      List<Parameter> parameters =
          injectableConstructors.stream()
              .flatMap(constructor -> Stream.of(constructor.getParameters()))
              .collect(Collectors.toUnmodifiableList());
      parameters.forEach(
          parameter -> {
            SemgrepScan semgrepScanAnnotation = parameter.getAnnotation(SemgrepScan.class);
            if (semgrepScanAnnotation != null) {
              if (!RuleSarif.class.equals(parameter.getType())) {
                throw new IllegalArgumentException(
                    "Can only inject semgrep results into "
                        + RuleSarif.class.getSimpleName()
                        + " types");
              }
              String yamlPath = semgrepScanAnnotation.pathToYaml();
              if ("".equals(yamlPath)) {
                yamlPath =
                    "/"
                        + codemodType.getPackageName().replace(".", "/")
                        + "/"
                        + semgrepScanAnnotation.ruleId()
                        + ".yaml";
                logger.info(
                    "Codemod {} didn't provide yaml path, assuming {}",
                    codemodType.getSimpleName(),
                    yamlPath);
              }
              yamlClasspathPathsToRun.add(yamlPath);
              toBind.add(semgrepScanAnnotation);
            }
          });

      if (toBind.isEmpty()) {
        logger.info("No Semgrep-based codemods, not running");
        return;
      }

      // copy the yaml out of the classpath onto disk so semgrep can use them
      List<Path> yamlRuleFiles =
          yamlClasspathPathsToRun.stream()
              .map(this::saveClasspathResourceToTemp)
              .collect(Collectors.toUnmodifiableList());

      // actually run the SARIF only once
      SarifSchema210 sarif;
      try {
        sarif = new DefaultSemgrepRunner().run(yamlRuleFiles, codeDirectory);
      } catch (IOException e) {
        throw new IllegalArgumentException("Semgrep execution failed", e);
      }

      // bind the SARIF results
      for (SemgrepScan sarifAnnotation : toBind) {
        SemgrepRuleSarif semgrepSarif = new SemgrepRuleSarif(sarifAnnotation.ruleId(), sarif);
        bind(RuleSarif.class).annotatedWith(sarifAnnotation).toInstance(semgrepSarif);
      }
    }
  }

  /**
   * Turn the yaml resource in the classpath into a file accessible via {@link Path}. Forgive the
   * exception re-throwing but this is being used from a lambda and this shouldn't fail ever anyway.
   */
  private Path saveClasspathResourceToTemp(final String yamlClasspathResourcePath) {
    try (InputStream ruleInputStream = getClass().getResourceAsStream(yamlClasspathResourcePath)) {
      Path semgrepRuleFile = Files.createTempFile("semgrep", ".yaml");
      Objects.requireNonNull(ruleInputStream);
      Files.copy(ruleInputStream, semgrepRuleFile, StandardCopyOption.REPLACE_EXISTING);
      ruleInputStream.close();
      return semgrepRuleFile;
    } catch (IOException e) {
      throw new UncheckedIOException("failed to write write yaml to disk", e);
    }
  }

  private static final Logger logger = LoggerFactory.getLogger(SemgrepModule.class);
}
