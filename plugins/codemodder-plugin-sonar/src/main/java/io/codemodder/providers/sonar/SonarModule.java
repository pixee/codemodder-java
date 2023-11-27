package io.codemodder.providers.sonar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.providers.sonar.api.Issue;
import io.codemodder.providers.sonar.api.SearchIssueResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

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

    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      final Constructor<?>[] constructors = codemodType.getDeclaredConstructors();

      List<Parameter> parameters =
          Stream.of(constructors)
              .filter(constructor -> constructor.getAnnotation(javax.inject.Inject.class) != null)
              .flatMap(constructor -> Stream.of(constructor.getParameters()))
              .filter(parameter -> parameter.getAnnotation(ProvidedSonarScan.class) != null)
              .toList();

      parameters.forEach(
          param -> {
            if (!RuleIssues.class.equals(param.getType())) {
              throw new IllegalArgumentException(
                  "can't use @ProvidedSonarScan on anything except RuleIssues (see "
                      + param.getDeclaringExecutable().getDeclaringClass().getName()
                      + ")");
            }
            // bind from existing scan
            ProvidedSonarScan annotation = param.getAnnotation(ProvidedSonarScan.class);
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
            }
          });
    }
  }

  private static final RuleIssues EMPTY = new DefaultRuleIssues(Map.of());
}
