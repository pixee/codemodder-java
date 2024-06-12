package io.codemodder.providers.sonar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SearchHotspotsResponse;
import io.codemodder.sonar.model.SearchIssueResponse;
import io.codemodder.sonar.model.SonarFinding;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.*;
import javax.inject.Inject;

final class SonarModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final Path repository;
  private final List<Path> issuesFiles;
  private final List<Path> hotspotsFiles;

  SonarModule(
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Path repository,
      final List<Path> sonarIssuesJsonFile,
      final List<Path> sonarHotspotsJsonPaths) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.repository = Objects.requireNonNull(repository);
    this.issuesFiles = sonarIssuesJsonFile;
    this.hotspotsFiles = sonarHotspotsJsonPaths;
  }

  @Override
  protected void configure() {
    configureFindings(RuleIssue.class, getIssues());
    configureFindings(RuleHotspot.class, getHotspots());
  }

  private void configureFindings(
      final Class<? extends RuleFinding> ruleFindingClass,
      final List<? extends SonarFinding> findings) {
    Map<String, List<SonarFinding>> findingsByRuleMap = groupFindingsByRule(findings);

    Set<String> packagesScanned = new HashSet<>();
    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      String packageName = codemodType.getPackageName();
      if (!packagesScanned.contains(packageName)) {
        packagesScanned.add(packageName);
        bindAnnotationsForPackage(ruleFindingClass, packageName, findingsByRuleMap);
      }
    }
  }

  private List<Issue> getIssues() {
    List<Issue> allIssues = List.of();
    if (issuesFiles != null) {
      List<SearchIssueResponse> issueResponses = new ArrayList<>();

      this.issuesFiles.forEach(
          issuesFile -> {
            try {
              SearchIssueResponse issueResponse =
                  new ObjectMapper()
                      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                      .readValue(issuesFile.toFile(), SearchIssueResponse.class);
              issueResponses.add(issueResponse);
            } catch (IOException e) {
              throw new UncheckedIOException("Problem reading Sonar issues JSON file", e);
            }
          });

      allIssues =
          issueResponses.stream().flatMap(response -> response.getIssues().stream()).toList();
    }

    return allIssues;
  }

  private List<Hotspot> getHotspots() {
    List<Hotspot> allHotspots = List.of();
    if (hotspotsFiles != null) {
      List<SearchHotspotsResponse> hotspotsResponses = new ArrayList<>();

      this.hotspotsFiles.forEach(
          hotspotsFile -> {
            try {
              SearchHotspotsResponse hotspotResponse =
                  new ObjectMapper()
                      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                      .readValue(hotspotsFile.toFile(), SearchHotspotsResponse.class);
              hotspotsResponses.add(hotspotResponse);
            } catch (IOException e) {
              throw new UncheckedIOException("Problem reading Sonar hotspot JSON file", e);
            }
          });

      allHotspots =
          hotspotsResponses.stream().flatMap(response -> response.getHotspots().stream()).toList();
    }

    return allHotspots;
  }

  private Map<String, List<SonarFinding>> groupFindingsByRule(
      List<? extends SonarFinding> findings) {
    Map<String, List<SonarFinding>> findingsByRuleMap = new HashMap<>();
    findings.forEach(
        finding -> {
          String rule = finding.rule();
          List<SonarFinding> perRuleList =
              findingsByRuleMap.computeIfAbsent(rule, k -> new ArrayList<>());
          perRuleList.add(finding);
        });
    return findingsByRuleMap;
  }

  private void bindAnnotationsForPackage(
      final Class<? extends RuleFinding> ruleFindingClass,
      String packageName,
      Map<String, List<SonarFinding>> findingsByRuleMap) {
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
      Map<String, List<SonarFinding>> findingsByRuleMap,
      Map<String, Boolean> boundRuleIds) {
    if (!RuleIssue.class.equals(param.getType()) && !RuleHotspot.class.equals(param.getType())) {
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

    // bind from existing scan
    List<SonarFinding> findings = findingsByRuleMap.get(annotation.ruleId());

    if (findings == null || findings.isEmpty()) {
      if (RuleIssue.class.equals(param.getType())) {
        bind(RuleIssue.class).annotatedWith(annotation).toInstance(EMPTY_RULE_ISSUES);
      } else {
        bind(RuleHotspot.class).annotatedWith(annotation).toInstance(EMPTY_RULE_HOTSPOT);
      }

    } else {
      RuleFinding ruleFinding = createRuleFindings(findings);

      if (RuleIssue.class.equals(param.getType())) {
        bind(RuleIssue.class).annotatedWith(annotation).toInstance((RuleIssue) ruleFinding);
      } else {
        bind(RuleHotspot.class).annotatedWith(annotation).toInstance((RuleHotspot) ruleFinding);
      }

      // Mark the ruleId as bound
      boundRuleIds.put(ruleId, true);
    }
  }

  private RuleFinding createRuleFindings(List<? extends SonarFinding> sonarFindings) {
    Map<String, List<SonarFinding>> findingsByPath = new HashMap<>();
    sonarFindings.forEach(
        finding -> {
          Optional<String> filename = finding.componentFileName();
          if (filename.isPresent()) {
            String fullPath = repository.resolve(filename.get()).toString();
            List<SonarFinding> pathFindings =
                findingsByPath.computeIfAbsent(fullPath, f -> new ArrayList<>());
            pathFindings.add(finding);
          }
        });
    return new RuleIssue(findingsByPath);
  }

  private static final RuleIssue EMPTY_RULE_ISSUES = new RuleIssue(Map.of());
  private static final RuleHotspot EMPTY_RULE_HOTSPOT = new RuleHotspot(Map.of());
}
