package io.codemodder.providers.sarif.pmd;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides PMD-related functionality to codemodder. */
public final class PmdProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      final Path codeDirectory,
      final List<Path> includedFiles,
      final List<String> includePaths,
      final List<String> excludePaths,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs,
      final Path sonarIssuesJsonFile) {
    return Set.of(new PmdModule(codeDirectory, includedFiles, codemodTypes));
  }
}
