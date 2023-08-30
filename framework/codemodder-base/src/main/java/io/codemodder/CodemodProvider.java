package io.codemodder;

import com.google.inject.AbstractModule;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * A type that helps provide functionality codemods. For instance, we may have providers that run
 * SAST tools, help codemods understand build files, dependency management, etc.
 */
public interface CodemodProvider {

  /**
   * Return a set of Guice modules that allow dependency injection
   *
   * @return a set of modules that perform dependency injection
   */
  Set<AbstractModule> getModules(
      Path repository, List<Class<? extends CodeChanger>> codemodTypes, List<RuleSarif> sarifs);

  /**
   * Tools this provider is interested in processing the SARIF output of. Codemodder CLI will look
   * for the SARIF outputted by tools in this list in the repository root and then provide the
   * results to {@link #getModules(Path, List, List)} as a {@link List} of {@link RuleSarif}s.
   *
   * <p>By default, this returns an empty list.
   *
   * @return a list of tool names that output SARIF that this provider wants to process
   */
  default List<String> wantsSarifToolNames() {
    return List.of();
  }
}
