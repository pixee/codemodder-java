package io.codemodder.plugins.maven.operator;

import com.github.zafarkhaja.semver.Version;

/** Represents a response to a version query, containing source and target version information. */
class VersionQueryResponse {

  private final Version source;
  private final Version target;

  /**
   * Constructs a new VersionQueryResponse with the specified source and target versions.
   *
   * @param source The source version information.
   * @param target The target version information.
   */
  public VersionQueryResponse(Version source, Version target) {
    this.source = source;
    this.target = target;
  }

  public Version getSource() {
    return source;
  }

  public Version getTarget() {
    return target;
  }
}
