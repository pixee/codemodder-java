package io.codemodder.sonar.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** The response when searching for hotspots. */
public final class SearchHotspotsResponse extends SearchFindingResponse {

  @JsonProperty("hotspots")
  private List<Hotspot> hotspots;

  public List<Hotspot> getHotspots() {
    return hotspots;
  }

  @Override
  public int findingCount() {
    return hotspots.size();
  }
}
