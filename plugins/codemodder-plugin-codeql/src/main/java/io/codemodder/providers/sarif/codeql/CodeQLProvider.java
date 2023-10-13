package io.codemodder.providers.sarif.codeql;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides codemods that act on CodeQL results. */
public final class CodeQLProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      final Path repository,
      final List<Path> includedFiles,
      final List<String> includePaths,
      final List<String> excludePaths,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs) {
    return Set.of(new CodeQLModule(codemodTypes, sarifs));
  }

  @Override
  public List<String> wantsSarifToolNames() {
    return List.of("CodeQL");
  }
}
