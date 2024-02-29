package io.codemodder.providers.sarif.appscan;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides codemods that act on AppScan results. */
public final class AppScanProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      final Path repository,
      final List<Path> includedFiles,
      final List<String> includePaths,
      final List<String> excludePaths,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs,
      final Path sonarIssuesJsonFile) {
    return Set.of(new AppScanModule(codemodTypes, sarifs));
  }

  @Override
  public List<String> wantsSarifToolNames() {
    return List.of("HCL AppScan Static Analyzer");
  }
}
