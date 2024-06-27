package io.codemodder.remediation;

import io.codemodder.CodeChanger;
import io.codemodder.CodemodReporterStrategy;

/** Reporter strategies that are useful without mentioning specific APIs. */
public enum GenericRemediationMetadata {
  XXE("xxe"),
  JNDI("jndi-injection"),
  HEADER_INJECTION("header-injection"),
  REFLECTION_INJECTION("reflection-injection"),
  SQL_INJECTION("sql-injection");

  private final CodemodReporterStrategy reporter;

  GenericRemediationMetadata(final String dir) {
    this.reporter =
        CodemodReporterStrategy.fromClasspathDirectory(
            CodeChanger.class, "/generic-remediation-reports/" + dir);
  }

  /** Get the reporter strategy for this vulnerability class. */
  public CodemodReporterStrategy reporter() {
    return reporter;
  }
}
