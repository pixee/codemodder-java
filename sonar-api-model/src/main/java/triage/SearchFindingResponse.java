package triage;

import com.fasterxml.jackson.annotation.JsonProperty;

/** The response when searching for a type of Sonar issue. */
public abstract class SearchFindingResponse {

  @JsonProperty("total")
  private int total;

  @JsonProperty("paging")
  private Paging paging;

  public int getTotal() {
    return total;
  }

  public Paging getPaging() {
    return paging;
  }

  /** Return the count of issues. */
  public abstract int findingCount();
}
