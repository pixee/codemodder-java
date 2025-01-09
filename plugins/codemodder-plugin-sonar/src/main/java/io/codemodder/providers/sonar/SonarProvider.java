package io.codemodder.providers.sonar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import io.codemodder.sonar.model.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Provides Sonar functionality to codemodder. */
public final class SonarProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      final Path repository,
      final List<Path> includedFiles,
      final List<String> pathIncludes,
      final List<String> pathExcludes,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs,
      final List<Path> sonarJsonPaths,
      final Path defectDojoFindingsJsonFile,
      final Path contrastFindingsJsonPath) {

    List<Issue> issues = new ArrayList<>();
    List<Hotspot> hotspots = new ArrayList<>();
    if (sonarJsonPaths != null) {
      for (Path path : sonarJsonPaths) {
        try {
          CombinedSearchIssueAndHotspotResponse response =
              new ObjectMapper()
                  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                  .readValue(path.toFile(), CombinedSearchIssueAndHotspotResponse.class);

          if (response.getIssues() != null) {
            issues.addAll(response.getIssues());
          }
          if (response.getHotspots() != null) {
            hotspots.addAll(response.getHotspots());
          }
        } catch (IOException e) {
          throw new UncheckedIOException("Problem reading Sonar JSON file", e);
        }
      }
    }

    return Set.of(
        new SonarModule<>(codemodTypes, repository, List.copyOf(issues), RuleIssue.class),
        new SonarModule<>(codemodTypes, repository, List.copyOf(hotspots), RuleHotspot.class));
  }
}
