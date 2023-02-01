package io.openpixee.codemod;

import java.security.SecureRandom;
import java.util.Random;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.reference.CtTypeReference;

/**
 * Transformation that replaces uses of {@code new java.util.Random()} with {@code new
 * java.security.SecureRandom()} when possible.
 */
final class WeakPRNG extends AbstractProcessor<CtConstructorCall<Random>> {

  @Override
  public void process(final CtConstructorCall<Random> element) {
    if (!element.getArguments().isEmpty()) {
      return;
    }
    final var factory = getFactory();
    final CtTypeReference<Object> secureRandomTypeRef =
        factory.createCtTypeReference(SecureRandom.class);
    element.replace(factory.createConstructorCall(secureRandomTypeRef));
  }
}
