package io.codemodder;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class EmptyReporter implements CodemodReporterStrategy {
  @Override
  public String getSummary() {
    return "";
  }

  @Override
  public String getDescription() {
    return "";
  }

  @Override
  public Optional<String> getSourceControlUrl() {
    return Optional.empty();
  }

  @Override
  public String getChange(final Path path, final CodemodChange codeChange) {
    return "";
  }

  @Override
  public List<String> getReferences() {
    return List.of();
  }
}
