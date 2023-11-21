package io.codemodder.providers.sonar.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Holds the paging data. */
public class Paging {

  @JsonProperty("pageIndex")
  private int pageIndex;

  @JsonProperty("pageSize")
  private int pageSize;

  @JsonProperty("total")
  private int total;
}
