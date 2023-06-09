package io.codemodder.plugins.contrast;

import com.google.inject.AbstractModule;
import io.codemodder.*;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides Contrast results dependency management functions to codemods. */
public final class ContrastProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(Path repository, List<Class<? extends CodeChanger>> codemodTypes, List<RuleSarif> sarifs) {
    return Set.of(new ContrastAssessModule(repository, codemodTypes));
  }
}
