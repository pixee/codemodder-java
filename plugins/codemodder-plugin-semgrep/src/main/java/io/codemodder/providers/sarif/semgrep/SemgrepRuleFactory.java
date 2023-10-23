package io.codemodder.providers.sarif.semgrep;

import io.codemodder.CodeChanger;

/** A type that creates codemodder-ready Semgrep YAML rules ready for execution. */
interface SemgrepRuleFactory {

  /**
   * Given the user's configuration data, return a definition of a rule that can be used by
   * codemodder.
   */
  SemgrepRule createYaml(
      Class<? extends CodeChanger> codemodType,
      final SemgrepScan scanInfo,
      final String packageName);
}
