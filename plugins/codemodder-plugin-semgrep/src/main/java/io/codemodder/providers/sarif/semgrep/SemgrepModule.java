package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for binding Semgrep-related things. */
public final class SemgrepModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final Path codeDirectory;
  private final SemgrepRunner semgrepRunner;

  public SemgrepModule(
      final Path codeDirectory, final List<Class<? extends CodeChanger>> codemodTypes) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
    this.semgrepRunner = SemgrepRunner.createDefault();
  }

  @Override
  protected void configure() {

    /*
     * This sections holds some fair bit of the creation of the "magic" codemod authors get to run. Guice isn't very good
     * about injecting a dependency which depends on an annotation's property, so we have to do some of the leg work
     * to do that binding ourselves.
     */
    List<Path> yamlPathsToRun = new ArrayList<>();

    // find all @SemgrepScan annotations in their parameters and batch them up for running
    List<Pair<String, SemgrepScan>> toBind = new ArrayList<>();

    Set<String> packagesScanned = new HashSet<>();

    for (Class<? extends CodeChanger> codemodType : codemodTypes) {

      String packageName = codemodType.getPackageName();
      if (!packagesScanned.contains(packageName)) {
        final List<Parameter> targetedParams;
        try (ScanResult scan =
            new ClassGraph()
                .enableAllInfo()
                .acceptPackagesNonRecursive(packageName)
                .removeTemporaryFilesAfterScan()
                .scan()) {
          ClassInfoList classesWithMethodAnnotation =
              scan.getClassesWithMethodAnnotation(Inject.class);
          List<Class<?>> injectableClasses = classesWithMethodAnnotation.loadClasses();

          targetedParams =
              injectableClasses.stream()
                  .map(Class::getDeclaredConstructors)
                  .flatMap(Arrays::stream)
                  .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
                  .map(Executable::getParameters)
                  .flatMap(Arrays::stream)
                  .filter(param -> param.isAnnotationPresent(SemgrepScan.class))
                  .toList();
        }

        targetedParams.forEach(
            param -> {
              if (!RuleSarif.class.equals(param.getType())) {
                throw new IllegalArgumentException(
                    "can't use @SemgrepScan on anything except RuleSarif (see "
                        + param.getDeclaringExecutable().getDeclaringClass().getName()
                        + ")");
              }

              SemgrepScan semgrepScan = param.getAnnotation(SemgrepScan.class);
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
                      "Cannot specify both inline yaml and yaml file path: "
                          + codemodType.getName());
                }
                foundYaml = true;
                yamlPathToWrite = saveStringToTemp(inlineYaml);
              }

              if (!foundYaml) {
                throw new IllegalArgumentException(
                    "no semgrep yaml found for: " + codemodType.getName());
              }

              try {
                if (StringUtils.isEmpty(declaredRuleId)) {
                  String rawYaml = Files.readString(yamlPathToWrite);
                  declaredRuleId = detectSingleRuleFromYaml(rawYaml);
                }
              } catch (IOException e) {
                throw new UncheckedIOException(
                    "Problem inspecting yaml: " + codemodType.getName(), e);
              }

              yamlPathsToRun.add(yamlPathToWrite);

              Pair<String, SemgrepScan> rulePair = Pair.of(declaredRuleId, semgrepScan);

              toBind.add(rulePair);
            });

        LOG.debug("Finished scanning codemod package: {}", packageName);
        packagesScanned.add(packageName);
      }
    }

    if (toBind.isEmpty()) {
      // no reason to run semgrep if there are no annotations
      return;
    }

    // fix up the yaml and add missing "message", "languages" and "severity" properties if they
    // aren't there
    for (Path yamlPath : yamlPathsToRun) {
      try {
        boolean changed = false;
        String yamlAsString = Files.readString(yamlPath);
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
          Files.writeString(yamlPath, yamlAsString, StandardOpenOption.TRUNCATE_EXISTING);
        }
      } catch (IOException e) {
        throw new UncheckedIOException("Problem fixing up yaml", e);
      }
    }

    // actually run the SARIF only once
    SarifSchema210 sarif;
    try {
      sarif = semgrepRunner.run(yamlPathsToRun, codeDirectory);
    } catch (IOException e) {
      throw new IllegalArgumentException("Semgrep execution failed", e);
    }

    // bind the SARIF results
    for (Pair<String, SemgrepScan> bindingPair : toBind) {
      SemgrepScan sarifAnnotation = bindingPair.getRight();
      SemgrepRuleSarif semgrepSarif = new SemgrepRuleSarif(bindingPair.getLeft(), sarif);
      bind(RuleSarif.class).annotatedWith(sarifAnnotation).toInstance(semgrepSarif);
    }

    // clean up all the temporary files
    for (Path yamlFile : yamlPathsToRun) {
      try {
        Files.delete(yamlFile);
      } catch (IOException e) {
        LOG.warn("Failed to delete temporary file: {}", yamlFile, e);
      }
    }
  }

  @VisibleForTesting
  String detectSingleRuleFromYaml(final String rawYaml) {
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
   * exception re-throwing but this is being used from a lambda and this shouldn't fail ever anyway.
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

  private static final Logger LOG = LoggerFactory.getLogger(SemgrepModule.class);
}
