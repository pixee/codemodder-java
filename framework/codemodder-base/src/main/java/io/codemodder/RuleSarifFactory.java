package io.codemodder;

import com.contrastsecurity.sarif.SarifSchema210;
import java.util.Optional;

/** Builds {@link RuleSarif}s. */
public interface RuleSarifFactory {

  /** Builds {@link RuleSarif}s if it supports {@code toolName}. */
  Optional<RuleSarif> build(
      String toolName, String rule, SarifSchema210 sarif, CodeDirectory codeDirectory);
}
