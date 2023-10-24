package io.codemodder.providers.sarif.semgrep.invalid.implicitbutmultiplerules;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.inject.Guice;
import com.google.inject.Injector;
import io.codemodder.providers.sarif.semgrep.SemgrepModule;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class UsesImplicitButHasMultipleRulesTest {

  @Test
  void it_fails_when_implicit_rule_but_multiple_specified(@TempDir Path tmpDir) {
    SemgrepModule module =
        new SemgrepModule(
            tmpDir, List.of("**"), List.of(), List.of(UsesImplicitButHasMultipleRules.class));
    Injector injector = Guice.createInjector(module);
    UsesImplicitButHasMultipleRules instance =
        injector.getInstance(UsesImplicitButHasMultipleRules.class);
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          instance.sarif.getRegionsFromResultsByRule(Path.of("anything"));
        });
  }
}
