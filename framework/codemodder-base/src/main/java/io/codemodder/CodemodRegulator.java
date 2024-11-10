package io.codemodder;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** A type that is relied on to inform our in-flight analysis on whether codemods are allowed. */
public interface CodemodRegulator {

  /**
   * Taking into account the configuration, understand if this codemod is currently allowed.
   *
   * @param codemodId the string id of the codemod
   * @return true, if the codemod is allowed -- false otherwise
   */
  boolean isAllowed(String codemodId);

  Optional<List<String>> desiredCodemodIdOrder();

  class DefaultCodemodRegulator implements CodemodRegulator {

    private final DefaultRuleSetting setting;
    private final List<String> exceptions;
    private final List<String> desiredOrder;

    DefaultCodemodRegulator(
        final DefaultRuleSetting defaultCodemodSetting, final List<String> codemodExceptions) {
      this.setting = Objects.requireNonNull(defaultCodemodSetting);
      this.exceptions = Objects.requireNonNull(codemodExceptions);
      if (DefaultRuleSetting.ENABLED.equals(defaultCodemodSetting)) {
        this.desiredOrder = null;
      } else {
        this.desiredOrder = codemodExceptions;
      }
    }

    @Override
    public boolean isAllowed(final String codemodId) {
      if (DefaultRuleSetting.ENABLED.equals(setting)) {
        return !exceptions.contains(codemodId);
      }
      return exceptions.contains(codemodId);
    }

    @Override
    public Optional<List<String>> desiredCodemodIdOrder() {
      return Optional.ofNullable(desiredOrder);
    }
  }

  static CodemodRegulator of(
      final DefaultRuleSetting defaultCodemodSetting, final List<String> codemodExceptions) {
    return new DefaultCodemodRegulator(defaultCodemodSetting, codemodExceptions);
  }
}
