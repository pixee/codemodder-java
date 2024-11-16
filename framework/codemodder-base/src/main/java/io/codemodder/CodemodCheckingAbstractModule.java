package io.codemodder;

import com.google.inject.AbstractModule;
import java.util.List;

/** A module that only configures if it is responsible for a codemod that's being loaded. */
public abstract class CodemodCheckingAbstractModule extends AbstractModule {

  private final boolean shouldActivate;

  /** Returns true if this module is responsible for any of the given codemods, so it doesn't . */
  protected CodemodCheckingAbstractModule(final List<Class<? extends CodeChanger>> codemodTypes) {
    this.shouldActivate = codemodTypes.stream().anyMatch(this::isResponsibleFor);
  }

  /** Returns true if this module is responsible for any of the given codemods, so it doesn't . */
  protected abstract boolean isResponsibleFor(Class<? extends CodeChanger> codemod);

  @Override
  protected final void configure() {
    if (shouldActivate) {
      doConfigure();
    }
  }

  /** Do the configuration that you would normally do in the configure method. */
  protected abstract void doConfigure();
}
