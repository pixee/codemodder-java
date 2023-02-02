package io.openpixee.codemod;

import com.contrastsecurity.sarif.SarifSchema210;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;

final class UntrustedServletForwardProcessor extends AbstractProcessor<CtInvocation<Void>> {

  public UntrustedServletForwardProcessor(final SarifSchema210 sarif) {}

  @Override
  public void process(final CtInvocation<Void> element) {}
}
