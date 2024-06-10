package io.codemodder.sonar.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.codemodder.sonar.model.update.FindingSeverity;

import java.util.List;
import java.util.Optional;

/** The base type for Sonar findings. */
public abstract class SonarFinding {

  @JsonProperty("key")
  private String key;

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

  @JsonProperty("severity")
  private SonarSeverity severity;

  @JsonProperty("message")
  private String message;

    @JsonProperty("flows")
    private List<Flow> flows;

    public List<Flow> getFlows() {
        return flows;
    }

  public FindingSeverity getSeverity() {
    return severity != null ? severity.toSeverity() : null;
  }

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

  /** Returns the type of the issue. */
  public abstract String typeName();
}
