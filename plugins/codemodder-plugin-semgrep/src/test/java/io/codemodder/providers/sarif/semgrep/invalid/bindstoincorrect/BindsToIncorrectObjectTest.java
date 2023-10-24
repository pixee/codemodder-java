package io.codemodder.providers.sarif.semgrep.invalid.bindstoincorrect;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.inject.CreationException;
import com.google.inject.Guice;
import io.codemodder.providers.sarif.semgrep.SemgrepModule;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class BindsToIncorrectObjectTest {

  @Test
  void it_fails_when_injecting_nonsarif_type(@TempDir Path tmpDir) {
    SemgrepModule module =
        new SemgrepModule(tmpDir, List.of("**"), List.of(), List.of(BindsToIncorrectObject.class));
    assertThrows(CreationException.class, () -> Guice.createInjector(module));
  }
}
