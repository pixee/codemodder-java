package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.LazyLoadingRuleSarif;
import io.codemodder.RuleSarif;
import io.github.classgraph.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.inject.Inject;
import javax.inject.Provider;
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
  private final SemgrepRuleFactory semgrepRuleFactory;

  public SemgrepModule(
      final Path codeDirectory,
      final List<String> includePatterns,
      final List<String> excludePatterns,
      final List<Class<? extends CodeChanger>> codemodTypes) {
    this(
        codeDirectory,
        includePatterns,
        excludePatterns,
        codemodTypes,
        List.of(),
        new DefaultSemgrepRuleFactory());
  }

  public SemgrepModule(
      final Path codeDirectory,
      final List<String> includePatterns,
      final List<String> excludePatterns,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs,
      final SemgrepRuleFactory semgrepRuleFactory) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
    this.includePatterns = Objects.requireNonNull(includePatterns);
    this.excludePatterns = Objects.requireNonNull(excludePatterns);
    this.semgrepRunner = SemgrepRunner.createDefault();
    this.sarifs = Objects.requireNonNull(sarifs);
    this.semgrepRuleFactory = Objects.requireNonNull(semgrepRuleFactory);
  }

  @Override
  protected void configure() {

    // find all the @ProvidedSemgrepScan annotations and bind them as is
    Set<String> packagesScanned = new HashSet<>();

    List<SemgrepRule> rules = new ArrayList<>();

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
              SemgrepRule rule =
                  semgrepRuleFactory.createRule(codemodType, semgrepScan, packageName);
              rules.add(rule);
            });

        LOG.trace("Finished scanning codemod package: {}", packageName);
        packagesScanned.add(packageName);
      }
    }

    /*
     * To avoid running semgrep and eating heavy, redundant file I/O for every codemod, we'll run it once with all rules, calculate which rules didn't "hit", and then storing an empty result for them. This will allow us to only run Semgrep on the rules for which we have evidence will hit. Given that we don't expect most projects to hit most codemods, this is a big time-savings.
     */
    final List<String> rawRulesFoundInBatchRun;
    try {
      SarifSchema210 allRulesSarif =
          semgrepRunner.run(
              rules.stream().map(SemgrepRule::yaml).toList(),
              codeDirectory,
              includePatterns,
              excludePatterns);
      rawRulesFoundInBatchRun =
          allRulesSarif.getRuns().get(0).getResults().stream().map(Result::getRuleId).toList();
    } catch (IOException e) {
      throw new UncheckedIOException("problem running batched execution", e);
    }

    for (SemgrepRule rule : rules) {
      SemgrepScan semgrepScan = rule.semgrepScan();
      if (rawRulesFoundInBatchRun.stream().anyMatch(r -> r.endsWith("." + rule.ruleId()))) {
        SemgrepSarifProvider semgrepSarifProvider =
            new SemgrepSarifProvider(
                codeDirectory,
                includePatterns,
                excludePatterns,
                semgrepRunner,
                rule.yaml(),
                rule.ruleId());
        LazyLoadingRuleSarif lazyLoadingRuleSarif = new LazyLoadingRuleSarif(semgrepSarifProvider);
        bind(RuleSarif.class).annotatedWith(semgrepScan).toInstance(lazyLoadingRuleSarif);
      } else {
        bind(RuleSarif.class).annotatedWith(semgrepScan).toInstance(RuleSarif.EMPTY);
      }
    }
  }

  private record SemgrepSarifProvider(
      Path codeDirectory,
      List<String> includePatterns,
      List<String> excludePatterns,
      SemgrepRunner semgrepRunner,
      Path yaml,
      String ruleId)
      implements Provider<RuleSarif> {

    SemgrepSarifProvider {
      Objects.requireNonNull(semgrepRunner);
      Objects.requireNonNull(codeDirectory);
      Objects.requireNonNull(includePatterns);
      Objects.requireNonNull(excludePatterns);
    }

    @Override
    public RuleSarif get() {

      // actually run the SARIF only once
      SarifSchema210 sarif;
      try {
        sarif = semgrepRunner.run(List.of(yaml), codeDirectory, includePatterns, excludePatterns);
      } catch (IOException e) {
        throw new IllegalArgumentException("Semgrep execution failed", e);
      }
      SingleSemgrepRuleSarif semgrepSarif =
          new SingleSemgrepRuleSarif(ruleId, sarif, codeDirectory);

      // clean up the temporary files
      try {
        Files.delete(yaml);
      } catch (IOException e) {
        LOG.warn("Failed to delete temporary file: {}", yaml, e);
      }

      return semgrepSarif;
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(SemgrepModule.class);
}
