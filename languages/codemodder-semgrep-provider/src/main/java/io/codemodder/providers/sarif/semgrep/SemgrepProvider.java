package io.codemodder.providers.sarif.semgrep;

import com.google.inject.AbstractModule;
import io.codemodder.Changer;
import io.codemodder.CodemodProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides Semgrep-related functionality to codemodder. */
public final class SemgrepProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      final Path codeDirectory, final List<Class<? extends Changer>> codemodTypes) {
    return Set.of(new SemgrepModule(codeDirectory, codemodTypes, new DefaultSemgrepRunner()));
  }
}
