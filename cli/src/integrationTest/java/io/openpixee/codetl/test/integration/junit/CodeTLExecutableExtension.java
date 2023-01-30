package io.openpixee.codetl.test.integration.junit;

import java.nio.file.Path;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit extension that injects a {@link CodeTLExecutable} into tests.
 *
 * <p>This extension is part of the seam between the test and the CodeTL program under test. This
 * seam allows for more flexibility in how we run CodeTL in the test suite.
 */
public final class CodeTLExecutableExtension implements ParameterResolver {

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return CodeTLExecutable.class.isAssignableFrom(parameterContext.getParameter().getType());
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    final Path executable =
        extensionContext
            .getConfigurationParameter(CONFIGURATION_PARAMETER_KEY)
            .map(Path::of)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Missing required configuration parameter " + CONFIGURATION_PARAMETER_KEY));
    return new CodeTLExecutable(executable);
  }

  private static final String CONFIGURATION_PARAMETER_KEY = "io.openpixee.codetl.test.executable";
}
