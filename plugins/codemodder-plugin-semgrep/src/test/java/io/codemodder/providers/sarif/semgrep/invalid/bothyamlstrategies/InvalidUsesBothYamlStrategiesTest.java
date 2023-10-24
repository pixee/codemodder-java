package io.codemodder.providers.sarif.semgrep.invalid.bothyamlstrategies;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import io.codemodder.providers.sarif.semgrep.SemgrepModule;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class InvalidUsesBothYamlStrategiesTest {

  @Test
  void it_fails_when_using_both_strategies(@TempDir Path tmpDir) {
    SemgrepModule module =
        new SemgrepModule(
            tmpDir, List.of("**"), List.of(), List.of(InvalidUsesBothYamlStrategies.class));
    assertThrows(CreationException.class, () -> Guice.createInjector(module));
  }
}
