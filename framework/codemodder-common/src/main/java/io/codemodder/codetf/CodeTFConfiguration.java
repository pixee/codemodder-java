package io.codemodder.codetf;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Describes the "configuration" section of the CCF report. */
public final class CodeTFConfiguration {

  private final String directory;

  private final List<String> includes;

  private final List<String> excludes;

  private final List<String> modules;

  private final List<CodeTFInput> inputs;

  @JsonCreator
  public CodeTFConfiguration(
      @JsonProperty("directory") final String directory,
      @JsonProperty("includes") final List<String> includes,
      @JsonProperty("excludes") final List<String> excludes,
      @JsonProperty("modules") final List<String> modules,
      @JsonProperty("inputs") final List<CodeTFInput> inputs) {
    this.directory = CodeTFValidator.requireNonBlank(directory);
    this.includes = CodeTFValidator.toImmutableCopyOrEmptyOnNull(includes);
    this.excludes = CodeTFValidator.toImmutableCopyOrEmptyOnNull(excludes);
    this.modules = CodeTFValidator.toImmutableCopyOrEmptyOnNull(modules);
    this.inputs = CodeTFValidator.toImmutableCopyOrEmptyOnNull(inputs);
  }

  public List<CodeTFInput> getInputs() {
    return inputs;
  }

  public List<String> getModules() {
    return modules;
  }

  public List<String> getExcludes() {
    return excludes;
  }

  public List<String> getIncludes() {
    return includes;
  }

  public String getDirectory() {
    return directory;
  }
}
