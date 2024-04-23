package io.codemodder.providers.sarif.appscan;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.CodeDirectory;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifFactory;
import java.util.Optional;

/** A factory for building {@link AppScanRuleSarif}s. */
public final class AppScanRuleSarifFactory implements RuleSarifFactory {

  @Override
  public Optional<RuleSarif> build(
      final String toolName,
      final String rule,
      final SarifSchema210 sarif,
      final CodeDirectory codeDirectory) {
    if (AppScanRuleSarif.toolName.equals(toolName)) {
      return Optional.of(new AppScanRuleSarif(rule, sarif, codeDirectory));
    }
    return Optional.empty();
  }
}
