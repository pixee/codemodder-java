package io.codemodder.providers.sarif.semgrep;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides Semgrep-related functionality to codemodder. */
public final class SemgrepProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      final Path codeDirectory,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs) {
    return Set.of(new SemgrepModule(codeDirectory, codemodTypes, sarifs));
  }

  @Override
  public List<String> wantsSarifToolNames() {
    return List.of("semgrep");
  }
}
