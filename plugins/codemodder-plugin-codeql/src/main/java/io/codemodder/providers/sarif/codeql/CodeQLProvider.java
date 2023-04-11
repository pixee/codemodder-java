package io.codemodder.providers.sarif.codeql;

import com.google.inject.AbstractModule;
import io.codemodder.Changer;
import io.codemodder.CodemodProvider;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides codemods that act on CodeQL results */
public class CodeQLProvider implements CodemodProvider {
  @Override
  public Set<AbstractModule> getModules(
      Path repository, List<Class<? extends Changer>> codemodTypes) {
    return Set.of(new CodeQLModule(codemodTypes));
  }
}
