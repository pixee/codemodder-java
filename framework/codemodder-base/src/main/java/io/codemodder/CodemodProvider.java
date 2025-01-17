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
   * @param repository the repository root
   * @param includedFiles the files that qualify for inclusion based on the patterns provided
   * @param pathIncludes the path includes provided to the CLI (which could inform the providers on
   *     their own analysis)
   * @param pathExcludes the path excludes provided to the CLI (which could inform the providers on
   *     their own analysis)
   * @param codemodTypes the codemod types that are being run
   * @param sarifs the SARIF output of tools that are being run
   * @param sonarJsonPaths the path to a Sonar issues/hotspots or combined JSON file retrieved from
   *     their web API -- may be null
   * @param contrastFindingsJsonPath the path to a Contrast findings JSON file retrieved from their
   *     web API -- may be null
   * @return a set of modules that perform dependency injection
   */
  Set<AbstractModule> getModules(
      Path repository,
      List<Path> includedFiles,
      List<String> pathIncludes,
      List<String> pathExcludes,
      List<Class<? extends CodeChanger>> codemodTypes,
      List<RuleSarif> sarifs,
      List<Path> sonarJsonPaths,
      Path defectDojoFindingsJsonPath,
      Path contrastFindingsJsonPath);

  /**
   * Tools this provider is interested in processing the SARIF output of. Codemodder CLI will look
   * for the SARIF outputted by tools in this list in the repository root and then provide the
   * results to {@link #getModules(Path, List, List, List, List, List, List, Path, Path)} as a
   * {@link List} of {@link RuleSarif}s.
   *
   * <p>By default, this returns an empty list.
   *
   * @return a list of tool names that output SARIF that this provider wants to process
   */
  default List<String> wantsSarifToolNames() {
    return List.of();
  }
}
