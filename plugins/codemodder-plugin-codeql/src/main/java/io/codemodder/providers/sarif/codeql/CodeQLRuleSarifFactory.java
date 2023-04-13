package io.codemodder.providers.sarif.codeql;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifFactory;
import java.nio.file.Path;
import java.util.Optional;

/** A factory for building {@link CodeQLRuleSarif}s. */
public class CodeQLRuleSarifFactory implements RuleSarifFactory {

  @Override
  public Optional<RuleSarif> build(
      String toolName, String rule, SarifSchema210 sarif, Path repositoryRoot) {
    if (toolName.equals(CodeQLRuleSarif.toolName)) {
      return Optional.of(new CodeQLRuleSarif(rule, sarif, repositoryRoot));
    }
    return Optional.empty();
  }
}
