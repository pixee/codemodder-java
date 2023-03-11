package io.codemodder.providers.sarif.semgrep;

import com.google.inject.AbstractModule;

final class SemgrepSarifModule extends AbstractModule {

  @Override
  protected void configure() {
    bind(SemgrepSarifProvider.class).toInstance(new DefaultSemgrepSarifProvider());
  }
}
