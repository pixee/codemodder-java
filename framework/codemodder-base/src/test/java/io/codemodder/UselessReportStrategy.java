package io.codemodder;

import java.nio.file.Path;
import java.util.List;

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
  public String getChange(Path path, CodemodChange change) {
    return "change";
  }

  @Override
  public List<String> getReferences() {
    return List.of();
  }
}
