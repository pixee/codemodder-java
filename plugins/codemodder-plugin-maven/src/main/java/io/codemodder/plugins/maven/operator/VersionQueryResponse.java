package io.codemodder.plugins.maven.operator;

import com.github.zafarkhaja.semver.Version;

class VersionQueryResponse {

  private final Version source;
  private final Version target;

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
