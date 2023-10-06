package io.codemodder;

import java.nio.file.Path;
import java.util.List;

/** A reporter with empty strings for testing purposes. */
public final class EmptyReporter implements CodemodReporterStrategy {
  @Override
  public String getSummary() {
    return "";
  }

  @Override
  public String getDescription() {
    return "";
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
