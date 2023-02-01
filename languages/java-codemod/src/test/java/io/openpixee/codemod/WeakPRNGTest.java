package io.openpixee.codemod;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spoon.SpoonAPI;

/** Unit tests for {@link WeakPRNG}. */
final class WeakPRNGTest {

  private SpoonAPI spoon;
  private Path output;

  @BeforeEach
  void before(@TempDir final Path tmp) throws IOException {
    var source =
        """
      import java.util.Random;

      interface RandomVulnerability {

        default void hasThing() {
          Random random = new Random();
          random = new Random(100L); // can't touch this one because SecureRandom has no similar signature
          random = new Random(getLong()); // same
        }

        long getLong();
      }"""
            .stripIndent();

    spoon = SpoonAPIFactory.create();
    spoon.addProcessor(new WeakPRNG());
    output = tmp.resolve("output");
    spoon.setSourceOutputDirectory(output.toFile());

    final Path input = tmp.resolve("input");
    Files.createDirectory(input);
    final Path file = input.resolve("RandomVulnerability.java");
    Files.writeString(file, source);

    spoon.addInputResource(file.toString());
  }

  @Test
  void transform_method_with_random_ctor_calls() {
    spoon.run();
    final Path transformed = output.resolve("RandomVulnerability.java");

    // FIXME https://github.com/INRIA/spoon/issues/4070
    var expected =
        """
      import java.util.Random;

      interface RandomVulnerability {

        default void hasThing() {
          Random random = new java.security.SecureRandom();
          random = new Random(100L); // can't touch this one because SecureRandom has no similar signature
          random = new Random(getLong()); // same
        }

        long getLong();
      }"""
            .stripIndent();
    assertThat(transformed).hasContent(expected);
  }
}
