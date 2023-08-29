package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifFactory;
import java.nio.file.Path;
import java.util.Optional;

/** A factory for building {@link SemgrepRuleSarif}s. */
public class SemgrepRuleSarifFactory implements RuleSarifFactory {

  @Override
  public Optional<RuleSarif> build(
      String toolName, String rule, SarifSchema210 sarif, Path repositoryRoot) {
    if (SemgrepRuleSarif.toolName.equals(toolName)) {
      return Optional.of(new SemgrepRuleSarif(rule, sarif));
    }
    return Optional.empty();
  }
}
