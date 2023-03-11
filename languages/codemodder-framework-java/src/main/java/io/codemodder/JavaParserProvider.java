package io.codemodder;

import com.google.inject.AbstractModule;
import java.util.Optional;
import java.util.Set;

/** Turns Java files into. */
public class JavaParserProvider implements CodemodProvider {

  @Override
  public Set<AbstractModule> getModules() {
    return Set.of();
  }

  @Override
  public Optional<ChangerHandler> getChangerHandler(final Changer changer) {
    return Optional.empty();
  }
}
