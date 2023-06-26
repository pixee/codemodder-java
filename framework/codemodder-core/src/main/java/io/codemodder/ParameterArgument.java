package io.codemodder;

import com.fasterxml.jackson.annotation.JsonProperty;

class ParameterArgument {
  @JsonProperty("codemod")
  private String codemodId;

  @JsonProperty("file")
  private String file;

  @JsonProperty("line")
  private String line;

  @JsonProperty("name")
  private String name;

  @JsonProperty("value")
  private String value;

  public String getCodemodId() {
    return codemodId;
  }

  public String getFile() {
    return file;
  }

  public String getLine() {
    return line;
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }
}
