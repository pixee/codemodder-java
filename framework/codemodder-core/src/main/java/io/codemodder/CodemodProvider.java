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
}
