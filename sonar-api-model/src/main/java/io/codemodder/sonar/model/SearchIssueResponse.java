package io.codemodder.sonar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** The response when searching for issues. */
public final class SearchIssueResponse extends SearchFindingResponse {

  @JsonProperty("issues")
  private List<Issue> issues;

  public List<Issue> getIssues() {
    return issues;
  }

  @Override
  public int findingCount() {
    return issues.size();
  }
}
