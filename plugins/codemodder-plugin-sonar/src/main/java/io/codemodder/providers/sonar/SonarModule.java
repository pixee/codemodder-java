package io.codemodder.providers.sonar;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SonarFinding;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.*;
import javax.inject.Inject;

final class SonarModule<T extends SonarFinding> extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final Path repository;
  private final List<T> findingsFound;

  private final Class<? extends RuleFinding<T>> ruleFindingClass;

  SonarModule(
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Path repository,
      final List<T> findings,
      final Class<? extends RuleFinding<T>> ruleFindingClass) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.repository = Objects.requireNonNull(repository);
    this.findingsFound = findings;
    this.ruleFindingClass = ruleFindingClass;
  }

  @Override
  protected void configure() {

    Map<String, List<T>> findingsByRuleMap = groupFindingsByRule(findingsFound);

    Set<String> packagesScanned = new HashSet<>();
    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      String packageName = codemodType.getPackageName();
      if (!packagesScanned.contains(packageName)) {
        packagesScanned.add(packageName);
        bindAnnotationsForPackage(packageName, findingsByRuleMap);
      }
    }
  }

  private Map<String, List<T>> groupFindingsByRule(List<T> findings) {
    Map<String, List<T>> findingsByRuleMap = new HashMap<>();
    findings.forEach(
        finding -> {
          String rule = finding.rule();
          List<T> perRuleList = findingsByRuleMap.computeIfAbsent(rule, k -> new ArrayList<>());
          perRuleList.add(finding);
        });
    return findingsByRuleMap;
  }

  private void bindAnnotationsForPackage(
      String packageName, Map<String, List<T>> findingsByRuleMap) {
    try (ScanResult scan =
        new ClassGraph()
            .enableAllInfo()
            .acceptPackagesNonRecursive(packageName)
            .removeTemporaryFilesAfterScan()
            .scan()) {

      List<Parameter> injectableParams = getInjectableParameters(scan);

      // Outside the loop, create a map to track already bound ruleIds
      final Map<String, Boolean> boundRuleIds = new HashMap<>();

      injectableParams.forEach(
          param -> {
            ProvidedSonarScan annotation = param.getAnnotation(ProvidedSonarScan.class);
            if (annotation != null && ruleFindingClass.equals(param.getType())) {
              bindParam(annotation, param, findingsByRuleMap, boundRuleIds);
            }
          });
    }
  }

  private List<Parameter> getInjectableParameters(ScanResult scan) {
    ClassInfoList classesWithMethodAnnotation = scan.getClassesWithMethodAnnotation(Inject.class);
    List<Class<?>> injectableClasses = classesWithMethodAnnotation.loadClasses();
    return injectableClasses.stream()
        .map(Class::getDeclaredConstructors)
        .flatMap(Arrays::stream)
        .filter(constructor -> constructor.isAnnotationPresent(Inject.class))
        .map(Executable::getParameters)
        .flatMap(Arrays::stream)
        .toList();
  }

  private void bindParam(
      ProvidedSonarScan annotation,
      Parameter param,
      Map<String, List<T>> findingsByRuleMap,
      Map<String, Boolean> boundRuleIds) {

    // Ensure the parameter type is valid
    if (!RuleFinding.class.isAssignableFrom(param.getType())) {
      throw new IllegalArgumentException(
          "can't use @ProvidedSonarScan on anything except RuleFinding (see "
              + param.getDeclaringExecutable().getDeclaringClass().getName()
              + ")");
    }

    final String ruleId = annotation.ruleId();

    // Check if the ruleId has already been bound
    if (boundRuleIds.containsKey(ruleId)) {
      return; // Skip binding if already bound
    }

    // Retrieve findings for the ruleId
    List<T> findings = findingsByRuleMap.getOrDefault(ruleId, Collections.emptyList());

    if (RuleIssue.class.equals(ruleFindingClass)) {
      if (findings.isEmpty()) {
        bind(RuleIssue.class)
            .annotatedWith(annotation)
            .toInstance(new RuleIssue(List.of(), repository));
      } else {
        bind(RuleIssue.class)
            .annotatedWith(annotation)
            .toInstance(new RuleIssue((List<Issue>) findings, repository));
      }
    } else if (RuleHotspot.class.equals(ruleFindingClass)) {
      if (findings.isEmpty()) {
        bind(RuleHotspot.class)
            .annotatedWith(annotation)
            .toInstance(new RuleHotspot(List.of(), repository));
      } else {
        bind(RuleHotspot.class)
            .annotatedWith(annotation)
            .toInstance(new RuleHotspot((List<Hotspot>) findings, repository));
      }
    }

    // Mark the ruleId as bound
    boundRuleIds.put(ruleId, true);
  }
}
