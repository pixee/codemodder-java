package io.codemodder.providers.sarif.pmd;

import com.contrastsecurity.sarif.SarifSchema210;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.RuleSarif;
import io.github.classgraph.*;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.*;
import java.util.*;
import javax.inject.Inject;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Responsible for binding PMD-related things. */
public final class PmdModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final Path codeDirectory;
  private final PmdRunner pmdRunner;

  public PmdModule(
      final Path codeDirectory, final List<Class<? extends CodeChanger>> codemodTypes) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.codeDirectory = Objects.requireNonNull(codeDirectory);
    this.pmdRunner = PmdRunner.createDefault();
  }

  @Override
  protected void configure() {

    // find all @PmdScan annotations in their parameters and batch them up for running
    List<Pair<String, PmdScan>> toBind = new ArrayList<>();

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
              String ruleId = pmdScan.ruleId();
              Pair<String, PmdScan> rulePair = Pair.of(ruleId, pmdScan);
              toBind.add(rulePair);
            });

        LOG.debug("Finished scanning codemod package: {}", packageName);
        packagesScanned.add(packageName);
      }
    }

    if (toBind.isEmpty()) {
      // no reason to run pmd if there are no annotations
      return;
    }

    List<String> rules = toBind.stream().map(Pair::getLeft).toList();
    SarifSchema210 sarif = pmdRunner.run(rules, codeDirectory);

    // bind the SARIF results to individual codemods
    for (Pair<String, PmdScan> bindingPair : toBind) {
      PmdScan sarifAnnotation = bindingPair.getRight();
      PmdRuleSarif pmdSarif = new PmdRuleSarif(bindingPair.getLeft(), sarif);
      bind(RuleSarif.class).annotatedWith(sarifAnnotation).toInstance(pmdSarif);
    }
  }

  private static final Logger LOG = LoggerFactory.getLogger(PmdModule.class);
}
