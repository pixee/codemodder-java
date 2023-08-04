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
      final List<Class<? extends CodeChanger>> codemodTypes,
      final List<RuleSarif> sarifs) {
    return Set.of(new LLMServiceModule());
  }
}
