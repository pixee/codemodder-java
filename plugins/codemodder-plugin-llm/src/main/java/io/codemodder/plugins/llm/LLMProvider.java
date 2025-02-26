package io.codemodder.plugins.llm;

import com.google.inject.AbstractModule;
import io.codemodder.CodeChanger;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides LLM-related functionality to codemods. */
public final class LLMProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      final Path repository,
      final List<Path> includedFiles,
      final List<String> includePaths,
      final List<String> excludePaths,
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs,
      final List<Path> sonarJsonPaths,
      final Path defectDojoFindingsJsonFile,
      final Path contrastFindingsJsonPath) {
    return Set.of(new LLMServiceModule());
  }
}
