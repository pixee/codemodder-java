package io.codemodder.sonar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * The response when searching for issues, combined with hotspots. The metadata fields are ignored.
 */
public final class CombinedSearchIssueAndHotspotResponse extends SearchFindingResponse {

  @JsonProperty("issues")
  private List<Issue> issues;

  @JsonProperty("hotspots")
  private List<Hotspot> hotspots;

  public List<Hotspot> getHotspots() {
    return hotspots;
  }

  public List<Issue> getIssues() {
    return issues;
  }

  @Override
  public int findingCount() {
    throw new UnsupportedOperationException("Not implemented");
  }
}
