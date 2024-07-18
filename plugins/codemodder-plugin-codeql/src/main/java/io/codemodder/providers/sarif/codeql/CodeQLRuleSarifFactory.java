package io.codemodder.providers.sarif.codeql;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.CodeDirectory;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifFactory;
import java.util.Optional;

/** A factory for building {@link CodeQLRuleSarif}s. */
public final class CodeQLRuleSarifFactory implements RuleSarifFactory {

  @Override
  public Optional<RuleSarif> build(
      final String toolName,
      final String rule,
      final String messageText,
      final SarifSchema210 sarif,
      final CodeDirectory codeDirectory) {
    if (CodeQLRuleSarif.toolName.equals(toolName)) {
      return Optional.of(new CodeQLRuleSarif(rule, sarif, codeDirectory));
    }
    return Optional.empty();
  }
}
