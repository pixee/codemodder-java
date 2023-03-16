package io.codemodder;

import java.util.Objects;

/**
 * During analysis, this will be the default setting for every rule. All of the rules listed in the
 * 'exceptions' section of the configuration will be considered the other, opposite value.
 */
public enum DefaultRuleSetting {
  ENABLED("enabled"),
  DISABLED("disabled");

  private final String description;

  DefaultRuleSetting(final String description) {
    this.description = Objects.requireNonNull(description);
  }

  public String getDescription() {
    return description;
  }

  public static DefaultRuleSetting fromDescription(final String s) {
    for (DefaultRuleSetting value : values()) {
      if (value.getDescription().equalsIgnoreCase(s)) {
        return value;
      }
    }
    throw new IllegalArgumentException("unknown default rule setting");
  }
}
