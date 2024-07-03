package io.codemodder.providers.sarif.semgrep;

import static io.codemodder.providers.sarif.semgrep.SemgrepProvider.semgrepToolNames;

import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.CodeDirectory;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifFactory;
import java.util.Optional;

/** A factory for building {@link SingleSemgrepRuleSarif}s. */
public class SemgrepRuleSarifFactory implements RuleSarifFactory {

  @Override
  public Optional<RuleSarif> build(
      final String toolName,
      final String rule,
      final SarifSchema210 sarif,
      final CodeDirectory codeDirectory) {

    if (semgrepToolNames.stream()
        .map(String::toLowerCase)
        .toList()
        .contains(toolName.toLowerCase())) {
      return Optional.of(new SingleSemgrepRuleSarif(rule, sarif, codeDirectory.asPath()));
    }
    return Optional.empty();
  }
}
