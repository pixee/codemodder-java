package io.codemodder.providers.sarif.semgrep;

import io.codemodder.CodemodChange;
import io.codemodder.CodemodReporterStrategy;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

final class UselessReporter implements CodemodReporterStrategy {

  @Override
  public String getSummary() {
    return "summary";
  }

  @Override
  public String getDescription() {
    return "description";
  }

  @Override
  public Optional<String> getSourceControlUrl() {
    return Optional.empty();
  }

  @Override
  public String getChange(Path path, CodemodChange change) {
    return "change";
  }

  @Override
  public List<String> getReferences() {
    return List.of("https://ref1/");
  }
}
