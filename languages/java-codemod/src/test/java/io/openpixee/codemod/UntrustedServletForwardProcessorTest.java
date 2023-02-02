package io.openpixee.codemod;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.opentest4j.TestAbortedException;

final class UntrustedServletForwardProcessorTest {

  @Test
  void request_parameter_source(@TempDir final Path tmp) throws IOException {
    // create source file
    final var source =
        """
          class Foo {
            void a(final HttpServletRequest request) throws ServletException, IOException {
              var foo = request.getParameter("foo");
              request.getRequestDispatcher(foo).forward(null, null);
            }
          }
        """
            .stripIndent();
    final var input = tmp.resolve("input");
    Files.createDirectory(input);
    Files.writeString(input.resolve("Foo.java"), source);

    // create rules
    final var rules = tmp.resolve("rules");
    Files.createDirectory(rules);
    try (var is =
        Objects.requireNonNull(
            UntrustedServletForwardProcessor.class.getResourceAsStream(
                "/semgrep/io/openpixee/codemod/untrusted-forward.yml"))) {
      Files.copy(is, rules.resolve("untrusted-forward.yml"));
    }

    // create output directory
    final var output = tmp.resolve("output");
    Files.createDirectory(output);

    // run semgrep
    final Path outputFile = output.resolve("semgrep.sarif.json");
    final var semgrep =
        new ProcessBuilder(
                "semgrep",
                "--sarif",
                "--config=" + rules,
                "--output " + outputFile,
                input.toString())
            .start();
    final boolean exited;
    try {
      exited = semgrep.waitFor(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TestAbortedException("interrupted while awaiting semgrep", e);
    }
    assertThat(exited).withFailMessage("timeout waiting for semgrep to complete analysis").isTrue();

    // read SARIF
    final SarifSchema210 sarif;
    try (var reader = Files.newBufferedReader(outputFile)) {
      sarif = new ObjectMapper().readerFor(SarifSchema210.class).readValue(reader);
    }

    // TODO give sarif to the codemod
  }
}
