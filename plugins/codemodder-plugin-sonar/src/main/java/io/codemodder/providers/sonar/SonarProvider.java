package io.codemodder.providers.sonar;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.Issue;
import io.codemodder.sonar.model.SearchHotspotsResponse;
import io.codemodder.sonar.model.SearchIssueResponse;
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
      final List<Path> sonarIssuesJsonPaths,
      final List<Path> sonarHotspotsJsonPaths,
      final Path defectDojoFindingsJsonFile,
      final Path contrastFindingsJsonPath) {
    final List<Issue> issues = getIssues(sonarIssuesJsonPaths);
    final List<Hotspot> hotspots = getHotspots(sonarHotspotsJsonPaths);
    return Set.of(
        new SonarModule<>(codemodTypes, repository, issues, RuleIssue.class),
        new SonarModule<>(codemodTypes, repository, hotspots, RuleHotspot.class));
  }

  private List<Issue> getIssues(List<Path> sonarIssuesJsonPaths) {
    List<Issue> allIssues = List.of();
    if (sonarIssuesJsonPaths != null) {
      List<SearchIssueResponse> issueResponses = new ArrayList<>();

      sonarIssuesJsonPaths.forEach(
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

  private List<Hotspot> getHotspots(final List<Path> sonarHotspotsJsonPaths) {
    List<Hotspot> allHotspots = List.of();
    if (sonarHotspotsJsonPaths != null) {
      List<SearchHotspotsResponse> hotspotsResponses = new ArrayList<>();

      sonarHotspotsJsonPaths.forEach(
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
}
