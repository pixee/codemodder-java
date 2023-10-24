package io.codemodder.providers.sarif.semgrep;

import io.codemodder.CodeChanger;

/** A type that creates codemodder-ready Semgrep YAML rules ready for execution. */
interface SemgrepRuleFactory {

  /**
   * Given the user's configuration data, return a definition of a rule that can be used by
   * codemodder.
   */
  SemgrepRule createRule(
      Class<? extends CodeChanger> codemodType, SemgrepScan scanInfo, String packageName);
}
