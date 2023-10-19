package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.LazyLoadingRuleSarif;
import io.codemodder.RuleSarif;
import io.github.classgraph.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Provider;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for binding Semgrep-related things. */
public final class SemgrepModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final Path codeDirectory;
  private final SemgrepRunner semgrepRunner;
  private final List<RuleSarif> sarifs;
  private final List<String> includePatterns;
  private final List<String> excludePatterns;

  @VisibleForTesting
  SemgrepModule(
      final Path codeDirectory,
      final List<String> includePatterns,
      final List<String> excludePatterns,
      final List<Class<? extends CodeChanger>> codemodTypes) {
    this(codeDirectory, includePatterns, excludePatterns, codemodTypes, List.of());
  }

  public SemgrepModule(
      final Path codeDirectory,
      final List<String> includePatterns,
      final List<String> excludePatterns,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
    this.includePatterns = Objects.requireNonNull(includePatterns);
    this.excludePatterns = Objects.requireNonNull(excludePatterns);
    this.semgrepRunner = SemgrepRunner.createDefault();
    this.sarifs = Objects.requireNonNull(sarifs);
  }

  @Override
  protected void configure() {

    // find all the @ProvidedSemgrepScan annotations and bind them as is
    Set<String> packagesScanned = new HashSet<>();

    for (Class<? extends CodeChanger> codemodType : codemodTypes) {

      String packageName = codemodType.getPackageName();
      if (!packagesScanned.contains(packageName)) {
        final List<Parameter> targetedParamsForJustInTimeScanning;
        final List<Parameter> targetedParamsForOfflineScanning;
        try (ScanResult scan =
            new ClassGraph()
                .enableAllInfo()
                .acceptPackagesNonRecursive(packageName)
                .removeTemporaryFilesAfterScan()
                .scan()) {
          ClassInfoList classesWithMethodAnnotation =
              scan.getClassesWithMethodAnnotation(Inject.class);
          List<Class<?>> injectableClasses = classesWithMethodAnnotation.loadClasses();

          List<Parameter> injectableParams =
              injectableClasses.stream()
                  .map(Class::getDeclaredConstructors)
                  .flatMap(Arrays::stream)
                  .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                  .map(Executable::getParameters)
                  .flatMap(Arrays::stream)
                  .toList();

          targetedParamsForJustInTimeScanning =
              injectableParams.stream()
                  .filter(param -> param.isAnnotationPresent(SemgrepScan.class))
                  .toList();

          targetedParamsForOfflineScanning =
              injectableParams.stream()
                  .filter(param -> param.isAnnotationPresent(ProvidedSemgrepScan.class))
                  .toList();
        }

        // we can bind these right away, because the scan has already occurred
        targetedParamsForOfflineScanning.forEach(
            param -> {
              if (!RuleSarif.class.equals(param.getType())) {
                throw new IllegalArgumentException(
                    "can't use @ProvidedSemgrepScan on anything except RuleSarif (see "
                        + param.getDeclaringExecutable().getDeclaringClass().getName()
                        + ")");
              }
              // bind from existing SARIF
              ProvidedSemgrepScan annotation = param.getAnnotation(ProvidedSemgrepScan.class);
              RuleSarif ruleSarif =
                  sarifs.stream()
                      .filter(sarif -> sarif.getRule().equals(annotation.ruleId()))
                      .findFirst()
                      .orElse(RuleSarif.EMPTY);
              bind(RuleSarif.class).annotatedWith(annotation).toInstance(ruleSarif);
            });

        targetedParamsForJustInTimeScanning.forEach(
            param -> {
              if (!RuleSarif.class.equals(param.getType())) {
                throw new IllegalArgumentException(
                    "can't use @SemgrepScan on anything except RuleSarif (see "
                        + param.getDeclaringExecutable().getDeclaringClass().getName()
                        + ")");
              }

              SemgrepScan semgrepScan = param.getAnnotation(SemgrepScan.class);
              SemgrepSarifProvider semgrepSarifProvider =
                  new SemgrepSarifProvider(
                      codeDirectory,
                      semgrepScan,
                      codemodType,
                      semgrepRunner,
                      packageName,
                      includePatterns,
                      excludePatterns);
              LazyLoadingRuleSarif lazyLoadingRuleSarif =
                  new LazyLoadingRuleSarif(semgrepSarifProvider);
              bind(RuleSarif.class).annotatedWith(semgrepScan).toInstance(lazyLoadingRuleSarif);
            });

        LOG.trace("Finished scanning codemod package: {}", packageName);
        packagesScanned.add(packageName);
      }
    }

    // fix up the yaml and add missing "message", "languages" and "severity" properties if they
    // aren't there

  }

  private static class SemgrepSarifProvider implements Provider<RuleSarif> {

    private final SemgrepScan semgrepScan;
    private final SemgrepRunner semgrepRunner;
    private final String packageName;
    private final Path codeDirectory;
    private final List<String> includePatterns;
    private final List<String> excludePatterns;
    private final Class<? extends CodeChanger> codemodType;

    private SemgrepSarifProvider(
        final Path codeDirectory,
        final SemgrepScan semgrepScan,
        final Class<? extends CodeChanger> codemodType,
        final SemgrepRunner semgrepRunner,
        final String packageName,
        final List<String> includePatterns,
        final List<String> excludePatterns) {

      this.codemodType = Objects.requireNonNull(codemodType);
      this.semgrepScan = Objects.requireNonNull(semgrepScan);
      this.semgrepRunner = Objects.requireNonNull(semgrepRunner);
      this.packageName = Objects.requireNonNull(packageName);
      this.codeDirectory = Objects.requireNonNull(codeDirectory);
      this.includePatterns = Objects.requireNonNull(includePatterns);
      this.excludePatterns = Objects.requireNonNull(excludePatterns);
    }

    @Override
    public RuleSarif get() {
      String yamlPath = semgrepScan.pathToYaml();
      String declaredRuleId = semgrepScan.ruleId();
      Path yamlPathToWrite = null;
      boolean foundYaml = false;
      if (!declaredRuleId.isEmpty()) {
        String classpathYamlPath =
            "/" + packageName.replace(".", "/") + "/" + declaredRuleId + ".yaml";

        if (!"".equals(yamlPath)) {
          classpathYamlPath = yamlPath;
        }
        Optional<Path> path = saveClasspathResourceToTemp(codemodType, classpathYamlPath);
        if (path.isPresent()) {
          foundYaml = true;
          yamlPathToWrite = path.get();
        }
      }
      String inlineYaml = semgrepScan.yaml();
      if (!"".equals(inlineYaml)) {
        if (foundYaml) {
          throw new IllegalArgumentException(
              "Cannot specify both inline yaml and yaml file path: " + codemodType.getName());
        }
        foundYaml = true;
        yamlPathToWrite = saveStringToTemp(inlineYaml);
      }

      if (!foundYaml) {
        throw new IllegalArgumentException("no semgrep yaml found for: " + codemodType.getName());
      }

      try {
        if (StringUtils.isEmpty(declaredRuleId)) {
          String rawYaml = Files.readString(yamlPathToWrite);
          declaredRuleId = detectSingleRuleFromYaml(rawYaml);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("Problem inspecting yaml: " + codemodType.getName(), e);
      }

      try {
        addMissingPropertiesIfNeeded(yamlPathToWrite);
      } catch (IOException e) {
        throw new UncheckedIOException("Problem fixing up yaml: " + codemodType.getName(), e);
      }

      // actually run the SARIF only once
      SarifSchema210 sarif;
      try {
        sarif = semgrepRunner.run(yamlPathToWrite, codeDirectory, includePatterns, excludePatterns);
      } catch (IOException e) {
        throw new IllegalArgumentException("Semgrep execution failed", e);
      }
      SemgrepRuleSarif semgrepSarif = new SemgrepRuleSarif(declaredRuleId, sarif, codeDirectory);

      // clean up the temporary files
      try {
        Files.delete(yamlPathToWrite);
      } catch (IOException e) {
        LOG.warn("Failed to delete temporary file: {}", yamlPathToWrite, e);
      }

      return semgrepSarif;
    }

    /**
     * Fix up the yaml and add missing "message", "languages" and "severity" properties if they
     * aren't there. This makes rule writing easier.
     */
    private static void addMissingPropertiesIfNeeded(Path yamlPathToWrite) throws IOException {
      // add missing properties to the yaml
      boolean changed = false;
      String yamlAsString = Files.readString(yamlPathToWrite);
      if (!yamlAsString.contains("message:")) {
        changed = true;
        yamlAsString += "\n    message: Semgrep found a match\n";
      }
      if (!yamlAsString.contains("severity:")) {
        changed = true;
        yamlAsString += "\n    severity: WARNING\n";
      }
      if (!yamlAsString.contains("languages:")) {
        changed = true;
        yamlAsString += "\n    languages:\n      - java\n";
      }
      if (changed) {
        Files.writeString(yamlPathToWrite, yamlAsString, StandardOpenOption.TRUNCATE_EXISTING);
      }
    }

    /** Save the YAML string given to a temporary file. */
    private Path saveStringToTemp(final String yamlAsString) {
      try {
        Path file = Files.createTempFile("semgrep", ".yaml");
        Files.writeString(file, yamlAsString);
        return file;
      } catch (IOException e) {
        throw new UncheckedIOException("Problem saving yaml string to temp", e);
      }
    }

    /**
     * Turn the yaml resource in the classpath into a file accessible via {@link Path}. Forgive the
     * exception re-throwing but this is being used from a lambda and this shouldn't fail ever
     * anyway.
     */
    private Optional<Path> saveClasspathResourceToTemp(
        final Class<?> codemodType, final String yamlClasspathResourcePath) {
      InputStream ruleInputStream = codemodType.getResourceAsStream(yamlClasspathResourcePath);
      if (ruleInputStream == null) {
        return Optional.empty();
      }
      try {
        Path semgrepRuleFile = Files.createTempFile("semgrep", ".yaml");
        Objects.requireNonNull(ruleInputStream);
        Files.copy(ruleInputStream, semgrepRuleFile, StandardCopyOption.REPLACE_EXISTING);
        ruleInputStream.close();
        return Optional.of(semgrepRuleFile);
      } catch (IOException e) {
        throw new UncheckedIOException("Problem reading/copying semgrep yaml from classpath", e);
      } finally {
        IOUtils.closeQuietly(ruleInputStream);
      }
    }
  }

  @VisibleForTesting
  static String detectSingleRuleFromYaml(final String rawYaml) {
    String ruleIdStartToken = "- id:";
    int count = StringUtils.countMatches(rawYaml, ruleIdStartToken);
    if (count > 1) {
      throw new IllegalArgumentException(
          "Multiple rules found in yaml, must specify rule single rule id if implicit");
    } else if (count == 0) {
      throw new IllegalArgumentException(
          "No rules found in yaml, must specify rule single rule id if implicit");
    }
    int start = rawYaml.indexOf(ruleIdStartToken);
    int end = rawYaml.indexOf("\n", start);
    return rawYaml.substring(start + ruleIdStartToken.length(), end).trim();
  }

  private static final Logger LOG = LoggerFactory.getLogger(SemgrepModule.class);
}
