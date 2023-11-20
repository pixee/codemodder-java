package io.codemodder.providers.sonar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import io.codemodder.providers.sonar.api.SearchIssueResponse;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Objects;

final class SonarModule extends AbstractModule {

  private final Path issuesFile;

  SonarModule(final Path sonarIssuesJsonFile) {
    this.issuesFile = Objects.requireNonNull(sonarIssuesJsonFile);
  }

  @Override
  protected void configure() {

    SearchIssueResponse issueResponse;
    try {
      issueResponse = new ObjectMapper().readValue(issuesFile.toFile(), SearchIssueResponse.class);
    } catch (IOException e) {
      throw new UncheckedIOException("Problem reading Sonar issues JSON file", e);
    }
  }
}
