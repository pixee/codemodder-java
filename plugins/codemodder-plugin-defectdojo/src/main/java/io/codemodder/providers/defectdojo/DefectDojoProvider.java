package io.codemodder.providers.defectdojo;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides DefectDojo functionality to codemodder. */
public final class DefectDojoProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      final Path repository,
      final List<Path> includedFiles,
      final List<String> pathIncludes,
      final List<String> pathExcludes,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs,
      final Path sonarIssuesJsonPath,
      final Path defectDojoFindingsJsonFile) {
    return Set.of(new DefectDojoModule(codemodTypes, defectDojoFindingsJsonFile));
  }
}
