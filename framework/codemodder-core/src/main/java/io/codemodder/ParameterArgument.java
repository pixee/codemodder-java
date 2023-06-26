package io.codemodder;

import com.fasterxml.jackson.annotation.JsonProperty;

class ParameterArgument {
  @JsonProperty(value = "codemod", required = true)
  private String codemodId;

  @JsonProperty("file")
  private String file;

  @JsonProperty("line")
  private String line;

  @JsonProperty(value = "name", required = true)
  private String name;

  @JsonProperty(value = "value", required = true)
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
