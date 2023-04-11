package io.codemodder.providers.sarif.codeql;

import com.google.inject.AbstractModule;
import io.codemodder.Changer;
import io.codemodder.CodemodProvider;
import io.codemodder.RuleSarif;
import io.codemodder.WantsSarif;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/** Provides codemods that act on CodeQL results. */
@WantsSarif(toolNames = {"CodeQL"})
public final class CodeQLProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules(
      Path repository, List<Class<? extends Changer>> codemodTypes, List<RuleSarif> sarifs) {
    return Set.of(new CodeQLModule(codemodTypes, sarifs));
  }
}
