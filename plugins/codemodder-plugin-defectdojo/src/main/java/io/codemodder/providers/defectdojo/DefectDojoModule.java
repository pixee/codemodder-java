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
  private final Path defectDojoFindingsJsonFile;
  private final Path projectDir;

  DefectDojoModule(
      final List<Class<? extends CodeChanger>> codemodTypes,
      final Path projectDir,
      final Path defectDojoFindingsJsonFile) {
    this.codemodTypes = Objects.requireNonNull(codemodTypes);
    this.projectDir = Objects.requireNonNull(projectDir);
    this.defectDojoFindingsJsonFile = defectDojoFindingsJsonFile;
  }

  @Override
  protected void configure() {

    Findings findings;

    // if there was no file, we still have to bind an empty result
    if (defectDojoFindingsJsonFile == null) {
      findings = new Findings(List.of());
    } else {
      try {
        findings =
            new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(defectDojoFindingsJsonFile.toFile(), Findings.class);
      } catch (IOException e) {
        throw new UncheckedIOException("can't read defectdojo JSON", e);
      }
    }

    // create a RuleFinding instance for every rule id we come across in the findings object
    Map<String, List<Finding>> ruleFindingsMap = new HashMap<>();
    for (Finding finding : findings.getResults()) {
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
