package io.codemodder.providers.sarif.appscan;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifFactory;
import java.nio.file.Path;
import java.util.Optional;

/** A factory for building {@link AppScanRuleSarif}s. */
public final class AppScanRuleSarifFactory implements RuleSarifFactory {

  @Override
  public Optional<RuleSarif> build(
      String toolName, String rule, SarifSchema210 sarif, Path repositoryRoot) {
    if (AppScanRuleSarif.toolName.equals(toolName)) {
      return Optional.of(new AppScanRuleSarif(rule, sarif, repositoryRoot));
    }
    return Optional.empty();
  }
}
