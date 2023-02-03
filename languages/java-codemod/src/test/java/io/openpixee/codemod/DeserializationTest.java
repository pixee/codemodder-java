package io.openpixee.codemod;

import com.contrastsecurity.sarif.SarifSchema210;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spoon.SpoonAPI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit tests for {@link DeserializationProcessor}. */
final class DeserializationTest {

  private SpoonAPI spoon;
  private Path output;

  @BeforeEach
  void before(@TempDir final Path tmp) throws IOException {

    var source =
        """
      import java.io.ObjectInputStream;

      interface DeserializationVulnerability {

        default void isVulnerable(InputStream is) {
          ObjectInputStream ois = new ObjectInputStream(is);
          ois.readObject();
        }
        
        default void usesBuiltinFilter() {
          ObjectInputStream ois = new ObjectInputStream(is);
          ois.setObjectInputFilter(null);
          ois.readObject();
        }
        
        default void usesOurFilterFullyQualified() {
          ObjectInputStream ois = new ObjectInputStream(is);
          io.openpixee.security.ObjectInputFilters.enableObjectFilterIfUnprotected(ois);
          ois.readObject();
        }
        
        default void usesOurFilter() {
          ObjectInputStream ois = new ObjectInputStream(is);
          ObjectInputFilters.enableObjectFilterIfUnprotected(ois);
          ois.readObject();
        }
      }"""
            .stripIndent();

    final Path input = tmp.resolve("input");
    Files.createDirectory(input);
    final Path file = input.resolve("DeserializationVulnerability.java");
    Files.writeString(file, source);

    SarifSchema210 sarif = Semgrep.run(tmp, "/semgrep/io/openpixee/codemod/harden-java-deserialization.yml", file);

    spoon = SpoonAPIFactory.create();
    spoon.addProcessor(new DeserializationProcessor(sarif));
    output = tmp.resolve("output");
    spoon.setSourceOutputDirectory(output.toFile());
    spoon.addInputResource(file.toString());
  }

  @Test
  void transform_code_with_insecure_and_secure_deser_calls() {
    spoon.run();
    final Path transformed = output.resolve("DeserializationVulnerability.java");

    // FIXME https://github.com/INRIA/spoon/issues/4070
    var expected =
            """
          import java.io.ObjectInputStream;
    
          interface DeserializationVulnerability {
    
            default void isVulnerable(InputStream is) {
              ObjectInputStream ois = new ObjectInputStream(is);
              io.openpixee.security.ObjectInputFilters.enableObjectFilterIfUnprotected(ois);
              ois.readObject();
            }
            
            default void usesBuiltinFilter() {
              ObjectInputStream ois = new ObjectInputStream(is);
              ois.setObjectInputFilter(null);
              ois.readObject();
            }
            
            default void usesOurFilterFullyQualified() {
              ObjectInputStream ois = new ObjectInputStream(is);
              io.openpixee.security.ObjectInputFilters.enableObjectFilterIfUnprotected(ois);
              ois.readObject();
            }
            
            default void usesOurFilter() {
              ObjectInputStream ois = new ObjectInputStream(is);
              ObjectInputFilters.enableObjectFilterIfUnprotected(ois);
              ois.readObject();
            }
          }"""
            .stripIndent();
    assertThat(transformed).hasContent(expected);
  }
}
