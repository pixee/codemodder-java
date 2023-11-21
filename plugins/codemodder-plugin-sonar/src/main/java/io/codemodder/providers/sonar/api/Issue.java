package io.codemodder.providers.sonar.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;

/** Describes an issue */
public class Issue {

  @JsonProperty("key")
  private String key;

  @JsonProperty("rule")
  private String rule;

  @JsonProperty("line")
  private int line;

  @JsonProperty("effort")
  private String effort;

  @JsonProperty("status")
  private String status;

  @JsonProperty("resolution")
  private String resolution;

  @JsonProperty("textRange")
  private TextRange textRange;

  @JsonProperty("component")
  private String component;

  @JsonProperty("message")
  private String message;

  public int getLine() {
    return line;
  }

  public String getMessage() {
    return message;
  }

  public String getKey() {
    return key;
  }

  public TextRange getTextRange() {
    return textRange;
  }

  public String getEffort() {
    return effort;
  }

  public String getRule() {
    return rule;
  }

  public String getResolution() {
    return resolution;
  }

  public String getComponent() {
    return component;
  }

  public String getStatus() {
    return status;
  }

  /** Returns the file path section of the component. */
  public Optional<String> componentFileName() {
    if (component == null || component.isEmpty()) {
      return Optional.empty();
    }
    String[] parts = component.split(":");
    if (parts.length != 2) {
      return Optional.empty();
    }
    return Optional.of(parts[1]);
  }
}
