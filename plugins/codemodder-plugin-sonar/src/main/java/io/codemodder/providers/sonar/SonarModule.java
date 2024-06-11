package io.codemodder.providers.sonar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SearchIssueResponse;
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
    configureIssues();
  }

  private void configureIssues() {
    final List<Issue> allIssues = getIssues();
    Map<String, List<Issue>> issuesByRuleMap = groupIssuesByRule(allIssues);

    Set<String> packagesScanned = new HashSet<>();
    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      String packageName = codemodType.getPackageName();
      if (!packagesScanned.contains(packageName)) {
        packagesScanned.add(packageName);
        bindAnnotationsForPackage(packageName, issuesByRuleMap);
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

  private Map<String, List<Issue>> groupIssuesByRule(List<Issue> allIssues) {
    Map<String, List<Issue>> issuesByRuleMap = new HashMap<>();
    allIssues.forEach(
        issue -> {
          String rule = issue.getRule();
          List<Issue> perRuleList = issuesByRuleMap.computeIfAbsent(rule, k -> new ArrayList<>());
          perRuleList.add(issue);
        });
    return issuesByRuleMap;
  }

  private void bindAnnotationsForPackage(
      String packageName, Map<String, List<Issue>> issuesByRuleMap) {
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
            if (annotation != null) {
              bindParam(annotation, param, issuesByRuleMap, boundRuleIds);
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
      Map<String, List<Issue>> issuesByRuleMap,
      Map<String, Boolean> boundRuleIds) {
    if (!RuleIssues.class.equals(param.getType())) {
      throw new IllegalArgumentException(
          "can't use @ProvidedSonarScan on anything except RuleIssues (see "
              + param.getDeclaringExecutable().getDeclaringClass().getName()
              + ")");
    }

    final String ruleId = annotation.ruleId();

    // Check if the ruleId has already been bound
    if (boundRuleIds.containsKey(ruleId)) {
      return; // Skip binding if already bound
    }

    // bind from existing scan
    List<Issue> issues = issuesByRuleMap.get(annotation.ruleId());

    if (issues == null || issues.isEmpty()) {
      bind(RuleIssues.class).annotatedWith(annotation).toInstance(EMPTY);
    } else {
      RuleIssues ruleIssues = createRuleIssues(issues);
      bind(RuleIssues.class).annotatedWith(annotation).toInstance(ruleIssues);
      // Mark the ruleId as bound
      boundRuleIds.put(ruleId, true);
    }
  }

  private RuleIssues createRuleIssues(List<Issue> issues) {
    Map<String, List<Issue>> issuesByPath = new HashMap<>();
    issues.forEach(
        issue -> {
          Optional<String> filename = issue.componentFileName();
          if (filename.isPresent()) {
            String fullPath = repository.resolve(filename.get()).toString();
            List<Issue> pathIssues = issuesByPath.computeIfAbsent(fullPath, f -> new ArrayList<>());
            pathIssues.add(issue);
          }
        });
    return new DefaultRuleIssues(issuesByPath);
  }

  private static final RuleIssues EMPTY = new DefaultRuleIssues(Map.of());
}
