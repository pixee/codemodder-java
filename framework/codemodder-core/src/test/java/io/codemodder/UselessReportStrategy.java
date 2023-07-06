package io.codemodder;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/** Reports static text for all */
public final class UselessReportStrategy implements CodemodReporterStrategy {

  public static UselessReportStrategy INSTANCE = new UselessReportStrategy();

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
    return List.of();
  }
}
