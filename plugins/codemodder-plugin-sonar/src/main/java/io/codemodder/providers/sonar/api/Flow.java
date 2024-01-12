package io.codemodder.providers.sonar.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public final class Flow {
  @JsonProperty("locations")
  private List<Location> locations;

  public List<Location> getLocations() {
    return locations;
  }

  public void setLocations(List<Location> locations) {
    this.locations = locations;
  }

  public static class Location {
    @JsonProperty("component")
    private String component;

    @JsonProperty("textRange")
    private TextRange textRange;

    @JsonProperty("msg")
    private String msg;

    public String getComponent() {
      return component;
    }

    public void setComponent(String component) {
      this.component = component;
    }

    public TextRange getTextRange() {
      return textRange;
    }

    public void setTextRange(TextRange textRange) {
      this.textRange = textRange;
    }

    public String getMsg() {
      return msg;
    }

    public void setMsg(String msg) {
      this.msg = msg;
    }
  }
}
