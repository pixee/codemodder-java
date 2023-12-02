package io.codemodder.providers.sonar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.providers.sonar.api.Issue;
import io.codemodder.providers.sonar.api.SearchIssueResponse;
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
  private final Path issuesFile;

  SonarModule(
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Path repository,
      final Path sonarIssuesJsonFile) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.repository = Objects.requireNonNull(repository);
    this.issuesFile = sonarIssuesJsonFile;
  }

  @Override
  protected void configure() {

    List<Issue> allIssues = List.of();
    if (issuesFile != null) {
      SearchIssueResponse issueResponse;
      try {
        issueResponse =
            new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(issuesFile.toFile(), SearchIssueResponse.class);
        allIssues = issueResponse.getIssues();
      } catch (IOException e) {
        throw new UncheckedIOException("Problem reading Sonar issues JSON file", e);
      }
    }

    Map<String, List<Issue>> perRuleBreakdown = new HashMap<>();
    allIssues.forEach(
        issue -> {
          String rule = issue.getRule();
          List<Issue> perRuleList =
              perRuleBreakdown.computeIfAbsent(rule, (k) -> new ArrayList<>());
          perRuleList.add(issue);
        });

    // find all the @ProvidedSemgrepScan annotations and bind them as is
    Set<String> packagesScanned = new HashSet<>();
    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      String packageName = codemodType.getPackageName();
      if (!packagesScanned.contains(packageName)) {
        packagesScanned.add(packageName);
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

          // Outside the loop, create a map to track already bound ruleIds
          final Map<String, Boolean> boundRuleIds = new HashMap<>();

          injectableParams.forEach(
              param -> {
                ProvidedSonarScan annotation = param.getAnnotation(ProvidedSonarScan.class);
                if (annotation == null) {
                  return;
                }
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
                List<Issue> issues = perRuleBreakdown.get(annotation.ruleId());

                if (issues == null || issues.isEmpty()) {
                  bind(RuleIssues.class).annotatedWith(annotation).toInstance(EMPTY);
                } else {
                  Map<String, List<Issue>> issuesByPath = new HashMap<>();
                  issues.forEach(
                      issue -> {
                        Optional<String> filename = issue.componentFileName();
                        if (filename.isPresent()) {
                          String fullPath = repository.resolve(filename.get()).toString();
                          List<Issue> pathIssues =
                              issuesByPath.computeIfAbsent(fullPath, (f) -> new ArrayList<>());
                          pathIssues.add(issue);
                        }
                      });
                  RuleIssues ruleIssues = new DefaultRuleIssues(issuesByPath);
                  bind(RuleIssues.class).annotatedWith(annotation).toInstance(ruleIssues);
                  // Mark the ruleId as bound
                  boundRuleIds.put(ruleId, true);
                }
              });
        }
      }
    }
  }

  private static final RuleIssues EMPTY = new DefaultRuleIssues(Map.of());
}
