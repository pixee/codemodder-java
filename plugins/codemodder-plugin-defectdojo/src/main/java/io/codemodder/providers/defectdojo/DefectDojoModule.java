package io.codemodder.providers.defectdojo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * Responsible for distributing the findings objects to DefectDojo based codemods based on rules.
 */
final class DefectDojoModule extends AbstractModule {

  private final List<Class<? extends CodeChanger>> codemodTypes;
  private final List<Path> defectDojoFindingsJsonFiles;
  private final Path projectDir;

  DefectDojoModule(
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Path projectDir,
      final List<Path> defectDojoFindingsJsonFiles) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.projectDir = Objects.requireNonNull(projectDir);
    this.defectDojoFindingsJsonFiles = defectDojoFindingsJsonFiles;
  }

  @Override
  protected void configure() {

    List<Findings> findings = new ArrayList<>();

    // if there was no file, we still have to bind an empty result
    if (defectDojoFindingsJsonFiles != null) {
      defectDojoFindingsJsonFiles.forEach(
          defectDojoFindingsJsonFile -> {
            try {
              Findings finding =
                  new ObjectMapper()
                      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                      .readValue(defectDojoFindingsJsonFile.toFile(), Findings.class);
              findings.add(finding);
            } catch (IOException e) {
              throw new UncheckedIOException("can't read defectdojo JSON", e);
            }
          });
    }

    final List<Finding> results =
        findings.stream().flatMap(finding -> finding.getResults().stream()).toList();

    // create a RuleFindings instance for every rule id we come across in the findings object
    Map<String, List<Finding>> ruleFindingsMap = new HashMap<>();
    for (Finding finding : results) {
      String ruleId = finding.getVulnIdFromTool();
      List<Finding> ruleFindings =
          ruleFindingsMap.computeIfAbsent(ruleId, (k) -> new ArrayList<>());
      ruleFindings.add(finding);
    }

    for (final Class<? extends CodeChanger> codemodType : codemodTypes) {
      final Constructor<?>[] constructors = codemodType.getDeclaredConstructors();
      final Optional<DefectDojoScan> annotation =
          Stream.of(constructors)
              .filter(constructor -> constructor.getAnnotation(javax.inject.Inject.class) != null)
              .flatMap(constructor -> Stream.of(constructor.getParameters()))
              .map(parameter -> parameter.getAnnotation(DefectDojoScan.class))
              .filter(Objects::nonNull)
              .findFirst();

      annotation.ifPresent(
          defectDojoScan -> {
            List<Finding> ruleFindings =
                ruleFindingsMap.getOrDefault(defectDojoScan.ruleId(), List.of());
            DefaultRuleFindings ruleFindingsToBind =
                new DefaultRuleFindings(ruleFindings, projectDir);
            bind(RuleFindings.class).annotatedWith(defectDojoScan).toInstance(ruleFindingsToBind);
          });
    }
  }
}
