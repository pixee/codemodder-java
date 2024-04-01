package io.codemodder.providers.defectdojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Represents the top level object in a DefectDojo findings API response. */
public class Findings {

  @JsonProperty("results")
  private List<Finding> results;

  public List<Finding> getResults() {
    return results;
  }
}
