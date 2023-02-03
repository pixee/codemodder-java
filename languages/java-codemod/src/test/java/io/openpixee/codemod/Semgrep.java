package io.openpixee.codemod;

import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentest4j.TestAbortedException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Utility for running semgrep. Assumes you've installed semgrep. Installing semgrep is easy:
 *
 * <code>pip install semgrep</code>
 */
final class Semgrep {

    /**
     *
     * @param tmp a temporary directory to use (that will be cleaned up on the caller's side)
     * @param rulePath the path to a classpath resource that contains the semgrep rule source
     * @param sourceFile the target file containing the source code being analyzed and eventually, maybe, transformed
     * @return the SARIF of the successful scan
     * @throws IOException on any issues running
     */
    static SarifSchema210 run(final Path tmp, final String rulePath, final Path sourceFile) throws IOException {
        // create rules
        final var rules = tmp.resolve("rules");
        Files.createDirectory(rules);
        try (var is =
                     Objects.requireNonNull(
                             UntrustedServletForwardProcessor.class.getResourceAsStream(
                                     rulePath))) { //"/semgrep/io/openpixee/codemod/untrusted-forward.yml"
            Files.copy(is, rules.resolve("my-tested-rule.yml"));
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
                        sourceFile.getParent().toString())
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
        return sarif;
    }
}
