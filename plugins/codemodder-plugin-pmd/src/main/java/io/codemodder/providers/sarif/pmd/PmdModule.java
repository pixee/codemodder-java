package io.codemodder.providers.sarif.pmd;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.SarifSchema210;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.LazyLoadingRuleSarif;
import io.codemodder.RuleSarif;
import io.github.classgraph.*;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.*;
import java.util.*;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for binding PMD-related things. */
public final class PmdModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final Path codeDirectory;
  private final PmdRunner pmdRunner;
  private final List<Path> includedFiles;

  public PmdModule(
      final Path codeDirectory,
      final List<Path> includedFiles,
      final List<Class<? extends CodeChanger>> codemodTypes) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
    this.includedFiles = Objects.requireNonNull(includedFiles);
    this.pmdRunner = PmdRunner.createDefault();
  }

  @Override
  protected void configure() {
    Set<String> packagesScanned = new HashSet<>();

    List<PmdScanTarget> scanTargets = new ArrayList<>();

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
                  .filter(param -> param.isAnnotationPresent(PmdScan.class))
                  .toList();
        }

        targetedParams.forEach(
            param -> {
              if (!RuleSarif.class.equals(param.getType())) {
                throw new IllegalArgumentException(
                    "can't use @PmdScan on anything except RuleSarif (see "
                        + param.getDeclaringExecutable().getDeclaringClass().getName()
                        + ")");
              }

              PmdScan pmdScan = param.getAnnotation(PmdScan.class);
              scanTargets.add(new PmdScanTarget(codemodType, pmdScan));
            });

        LOG.trace("Finished scanning codemod package: {}", packageName);
        packagesScanned.add(packageName);
      }
    }

    SarifSchema210 allRulesBatchedRun =
        pmdRunner.run(
            scanTargets.stream().map(PmdScanTarget::pmdScan).map(PmdScan::ruleId).toList(),
            codeDirectory,
            includedFiles);

    List<String> ruleIdsFoundInBatchScan =
        allRulesBatchedRun.getRuns().get(0).getResults().stream().map(Result::getRuleId).toList();

    for (PmdScanTarget scanTarget : scanTargets) {
      final String ruleId = scanTarget.pmdScan.ruleId();
      if (ruleIdsFoundInBatchScan.stream().noneMatch(r -> ruleId.endsWith("/" + r))) {
        LOG.trace("Rule {} was not found in batch scan, skipping", ruleId);
        bind(RuleSarif.class).annotatedWith(scanTarget.pmdScan).toInstance(RuleSarif.EMPTY);
        continue;
      }
      RuleSarif sarif =
          new LazyLoadingRuleSarif(
              () -> {
                LOG.trace("Running pmd for rule: {}", ruleId);
                SarifSchema210 rawSarifFromRun =
                    pmdRunner.run(List.of(ruleId), codeDirectory, includedFiles);
                LOG.trace("Finished running pmd for rule: {}", ruleId);
                int lastSlash = ruleId.lastIndexOf("/");
                if (lastSlash == -1) {
                  throw new IllegalStateException("unexpected rule id: " + ruleId);
                }
                String trimmedRuleId = ruleId.substring(lastSlash + 1);
                Map<String, List<Result>> resultsByFile = new HashMap<>();
                List<Result> allResults = rawSarifFromRun.getRuns().get(0).getResults();
                for (Result result : allResults) {
                  String filePath =
                      result
                          .getLocations()
                          .get(0)
                          .getPhysicalLocation()
                          .getArtifactLocation()
                          .getUri();
                  String normalizedFilePath =
                      filePath.startsWith("file://") ? filePath.substring(7) : filePath;
                  List<Result> resultsForFile =
                      resultsByFile.computeIfAbsent(normalizedFilePath, k -> new ArrayList<>());
                  resultsForFile.add(result);
                }
                return new PmdRuleSarif(trimmedRuleId, rawSarifFromRun, resultsByFile);
              });

      this.bind(RuleSarif.class).annotatedWith(scanTarget.pmdScan).toInstance(sarif);
    }
  }

  record PmdScanTarget(Class<? extends CodeChanger> codemodType, PmdScan pmdScan) {}

  private static final Logger LOG = LoggerFactory.getLogger(PmdModule.class);
}
