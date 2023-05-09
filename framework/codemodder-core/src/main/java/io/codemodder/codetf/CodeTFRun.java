package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Objects;

/** Describes the "run" section of a report. */
public final class CodeTFRun {

  private final String vendor;

  private final String tool;

  private final String version;

  private final String commandLine;

  private final Long elapsed;

  private final String directory;

  private final List<CodeTFSarifInput> sarifs;

  @JsonCreator
  public CodeTFRun(
      @JsonProperty("vendor") final String vendor,
      @JsonProperty("tool") final String tool,
      @JsonProperty("version") final String version,
      @JsonProperty("commandLine") final String commandLine,
      @JsonProperty("elapsed") final Long elapsed,
      @JsonProperty("directory") final String directory,
      @JsonProperty("sarifs") final List<CodeTFSarifInput> sarifs) {
    this.vendor = CodeTFValidator.requireNonBlank(vendor);
    this.tool = CodeTFValidator.requireNonBlank(tool);
    this.version = CodeTFValidator.requireNonBlank(version);
    this.commandLine = Objects.requireNonNull(commandLine);
    if (elapsed <= 0) {
      throw new IllegalArgumentException("elapsed must be a positive value");
    }
    this.elapsed = elapsed;
    this.directory = CodeTFValidator.requireNonBlank(directory);
    this.sarifs = CodeTFValidator.toImmutableCopyOrEmptyOnNull(sarifs);
  }

  public String getVendor() {
    return vendor;
  }

  public String getTool() {
    return tool;
  }

  public String getVersion() {
    return version;
  }

  public Long getElapsed() {
    return elapsed;
  }

  public String getCommandLine() {
    return commandLine;
  }

  public List<CodeTFSarifInput> getSarifs() {
    return sarifs;
  }

  public String getDirectory() {
    return directory;
  }
}
