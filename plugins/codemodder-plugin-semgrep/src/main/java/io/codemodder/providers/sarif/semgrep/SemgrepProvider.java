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
      final List<Path> includedFiles,
      final List<String> includePaths,
      final List<String> excludePaths,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs,
      final List<Path> sonarJsonPaths,
      final Path defectDojoFindingsJsonFile,
      final Path contrastFindingsJsonPath) {
    return Set.of(
        new SemgrepModule(
            codeDirectory,
            includePaths,
            excludePaths,
            codemodTypes,
            sarifs,
            new DefaultSemgrepRuleFactory()));
  }

  @Override
  public List<String> wantsSarifToolNames() {
    return semgrepToolNames;
  }

  static final List<String> semgrepToolNames = List.of("semgrep", "Semgrep", "Semgrep OSS");
}
