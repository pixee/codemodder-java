package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

public final class CodeTFPackageAction {

  public enum CodeTFPackageActionType {
    ADD,
    REMOVE
  }

  public enum CodeTFPackageActionResult {
    COMPLETED,
    FAILED,
    SKIPPED
  }

  private final String packageUrl;
  private final CodeTFPackageActionResult result;
  private final CodeTFPackageActionType action;

  @JsonCreator
  public CodeTFPackageAction(
      @JsonProperty("action") final CodeTFPackageActionType action,
      @JsonProperty("result") final CodeTFPackageActionResult result,
      @JsonProperty("package") final String packageUrl) {
    this.packageUrl = CodeTFValidator.requireNonBlank(packageUrl);
    this.result = Objects.requireNonNull(result);
    this.action = Objects.requireNonNull(action);
  }

  public String getPackageUrl() {
    return packageUrl;
  }

  public CodeTFPackageActionResult getResult() {
    return result;
  }

  public CodeTFPackageActionType getAction() {
    return action;
  }
}
