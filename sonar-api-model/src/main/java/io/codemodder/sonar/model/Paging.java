package io.codemodder.sonar.model;

import com.fasterxml.jackson.annotation.JsonProperty;

class Paging {

  @JsonProperty("pageIndex")
  int pageIndex;

  @JsonProperty("pageSize")
  int pageSize;

  @JsonProperty("total")
  int total;
}
