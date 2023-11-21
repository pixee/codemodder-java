package io.codemodder.providers.sonar.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The top level response from the issues API. */
public class SearchIssueResponse {

  @JsonProperty("paging")
  private Paging paging;

  @JsonProperty("issues")
  private List<Issue> issues;

  public Paging getPaging() {
    return paging;
  }

  public List<Issue> getIssues() {
    return issues;
  }
}
