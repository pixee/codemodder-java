package io.codemodder;

import java.util.List;
import java.util.Objects;

/** A type that is relied on to inform our in-flight analysis on whether codemods are allowed. */
public interface CodemodRegulator {

  /**
   * Taking into account the configuration, understand if this codemod is currently allowed.
   *
   * @param codemodId the string id of the codemod
   * @return true, if the codemod is allowed -- false otherwise
   */
  boolean isAllowed(String codemodId);

  class DefaultCodemodRegulator implements CodemodRegulator {

    private final DefaultRuleSetting setting;
    private final List<String> exceptions;

    DefaultCodemodRegulator(
        final DefaultRuleSetting defaultCodemodSetting, final List<String> codemodExceptions) {
      this.setting = Objects.requireNonNull(defaultCodemodSetting);
      this.exceptions = Objects.requireNonNull(codemodExceptions);
    }

    @Override
    public boolean isAllowed(final String codemodId) {
      if (DefaultRuleSetting.ENABLED.equals(setting)) {
        return !exceptions.contains(codemodId);
      }
      return exceptions.contains(codemodId);
    }
  }

  static CodemodRegulator of(
      final DefaultRuleSetting defaultCodemodSetting, final List<String> codemodExceptions) {
    return new DefaultCodemodRegulator(defaultCodemodSetting, codemodExceptions);
  }
}
