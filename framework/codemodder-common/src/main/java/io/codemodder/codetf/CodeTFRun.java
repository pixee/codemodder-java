package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Describes the "run" section of a CCF report. */
public final class CodeTFRun {

  private final String vendor;

  private final String tool;

  private final String commandLine;

  private final Long elapsed;

  private final CodeTFConfiguration configuration;

  private final List<CodeTFFileExtensionScanned> filesScanned;

  private final List<String> failedFiles;

  @JsonCreator
  public CodeTFRun(
      @JsonProperty("vendor") final String vendor,
      @JsonProperty("tool") final String tool,
      @JsonProperty("commandLine") final String commandLine,
      @JsonProperty("elapsed") final Long elapsed,
      @JsonProperty("fileExtensionsScanned") final List<CodeTFFileExtensionScanned> filesScanned,
      @JsonProperty("configuration") final CodeTFConfiguration configuration,
      @JsonProperty("failedFiles") final List<String> failedFiles) {
    this.vendor = CodeTFValidator.requireNonBlank(vendor);
    this.tool = CodeTFValidator.requireNonBlank(tool);
    this.commandLine = commandLine;

    if (elapsed <= 0) {
      throw new IllegalArgumentException("elapsed must be a positive value");
    }
    this.elapsed = elapsed;
    this.configuration = Objects.requireNonNull(configuration, "configuration");

    this.filesScanned = Objects.requireNonNullElse(filesScanned, Collections.emptyList());

    this.failedFiles = Objects.requireNonNullElse(failedFiles, Collections.emptyList());
  }

  public String getVendor() {
    return vendor;
  }

  public String getTool() {
    return tool;
  }

  public Long getElapsed() {
    return elapsed;
  }

  public String getCommandLine() {
    return commandLine;
  }

  public CodeTFConfiguration getConfiguration() {
    return configuration;
  }

  public List<String> getFailedFiles() {
    return failedFiles;
  }

  public List<CodeTFFileExtensionScanned> getFilesScanned() {
    return filesScanned;
  }
}
