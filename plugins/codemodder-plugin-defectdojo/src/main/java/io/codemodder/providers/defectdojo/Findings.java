package io.codemodder.providers.defectdojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/** Represents the top level object in a DefectDojo findings API response. */
public class Findings {

  public Findings() {
    // needed for deserialization
  }

  public Findings(final List<Finding> results) {
    this.results = Objects.requireNonNull(results);
  }

  @JsonProperty("results")
  private List<Finding> results;

  public List<Finding> getResults() {
    return results;
  }
}
