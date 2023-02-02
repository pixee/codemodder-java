package io.openpixee.codemod;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
    final Path fooSource = input.resolve("Foo.java");
    Files.writeString(fooSource, source);

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
                "scan",
                "--sarif",
                "--config",
                rules.toString(),
                "--output",
                outputFile.toString(),
                input.toString())
            .start();
    final var err = new String(semgrep.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
    final boolean exited;
    try {
      exited = semgrep.waitFor(2, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new TestAbortedException("interrupted while awaiting semgrep", e);
    }
    assertThat(exited).withFailMessage("timeout waiting for semgrep to complete analysis").isTrue();
    assertThat(semgrep.exitValue())
        .withFailMessage("semgrep execution failed:\n" + err)
        .isEqualTo(0);

    // read SARIF
    final SarifSchema210 sarif;
    try (var reader = Files.newBufferedReader(outputFile)) {
      sarif = new ObjectMapper().readerFor(SarifSchema210.class).readValue(reader);
    }

    // set-up spoon, passing the SARIF to the codemod
    final var spoon = SpoonAPIFactory.create();
    spoon.addProcessor(new UntrustedServletForwardProcessor(sarif));
    spoon.setSourceOutputDirectory(output.toFile());
    spoon.addInputResource(fooSource.toString());
  }
}
