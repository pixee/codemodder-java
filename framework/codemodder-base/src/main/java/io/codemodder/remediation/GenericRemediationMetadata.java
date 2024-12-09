package io.codemodder.remediation;

import io.codemodder.CodeChanger;
import io.codemodder.CodemodReporterStrategy;

/** Reporter strategies that are useful without mentioning specific APIs. */
public enum GenericRemediationMetadata {
  XXE("xxe"),
  XSS("xss"),
  JNDI("jndi-injection"),
  HEADER_INJECTION("header-injection"),
  REFLECTION_INJECTION("reflection-injection"),
  DESERIALIZATION("java-deserialization"),
  MISSING_SECURE_FLAG("missing-secure-flag"),
  SQL_INJECTION("sql-injection"),
  SSRF("ssrf"),
  WEAK_RANDOM("weak-random"),
  PREDICTABLE_SEED("predictable-seed"),
  ZIP_SLIP("zip-slip"),
  REGEX_INJECTION("regex-injection"),
  ERROR_MESSAGE_EXPOSURE("error-message-exposure"),
  LOG_INJECTION("log-injection"),
  WEAK_CRYPTO_ALGORITHM("weak-crypto-algorithm");

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
