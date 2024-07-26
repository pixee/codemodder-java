package io.codemodder.remediation.javadeserialization;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.DependencyGAV;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class DefaultJavaDeserializationRemediatorTest {

  private DefaultJavaDeserializationRemediator remediator;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    remediator = new DefaultJavaDeserializationRemediator();
    rule = new DetectorRule("untrusted-deserialization", "Untrusted Deserialization", null);
  }

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        Arguments.of(
            """
                            import java.io.ObjectInputStream;
                            class Foo {
                              void bar(ObjectInputStream ois) {
                                Acme acme = ois.readObject();
                              }
                            }
                            """,
            4,
            "Unexpected declaration type"),
        Arguments.of(
            """
                            import java.io.ObjectInputStream;
                            class Foo {
                              void bar() {
                                Acme acme = getOis().readObject();
                              }
                            }
                            """,
            4,
            "Unexpected shape"));
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_doesnt_handle_unfixables(final String badCode, final int line, final String reason) {
    CompilationUnit cu = StaticJavaParser.parse(badCode);
    LexicalPreservingPrinter.setup(cu);

    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "path", rule, List.of(new Object()), o -> "id", o -> line, o -> null);
    assertThat(result.unfixedFindings()).hasSize(1);
    assertThat(result.changes()).isEmpty();
    UnfixedFinding unfixedFinding = result.unfixedFindings().get(0);
    assertThat(unfixedFinding.getReason()).isEqualTo(reason);
    assertThat(unfixedFinding.getRule()).isEqualTo(rule);
    assertThat(unfixedFinding.getLine()).isEqualTo(line);
    assertThat(unfixedFinding.getPath()).isEqualTo("path");
  }

  @Test
  void it_fixes_java_deserialization() {

    String fixableCode =
        """
                package com.acme;
                import java.io.ObjectInputStream;
                import java.io.InputStream;

                class Foo {
                    Acme readAcme(InputStream is) {
                        ObjectInputStream ois = new ObjectInputStream(is);
                        // read the obj
                        Acme acme = (Acme) ois.readObject();
                        return acme;
                    }
                }
                """;

    CompilationUnit cu = StaticJavaParser.parse(fixableCode);
    LexicalPreservingPrinter.setup(cu);

    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "path", rule, List.of(new Object()), o -> "id", o -> 9, o -> null);
    assertThat(result.unfixedFindings()).isEmpty();
    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(9);
    List<FixedFinding> fixedFindings = change.getFixedFindings();
    assertThat(fixedFindings).hasSize(1);
    assertThat(change.getDependenciesNeeded()).containsExactly(DependencyGAV.JAVA_SECURITY_TOOLKIT);

    assertThat(fixedFindings.get(0).getId()).isEqualTo("id");
    assertThat(fixedFindings.get(0).getRule()).isEqualTo(rule);

    String afterCode = LexicalPreservingPrinter.print(cu);
    assertThat(afterCode)
        .isEqualToIgnoringWhitespace(
            """
                package com.acme;
                import static io.github.pixee.security.ObjectInputFilters.createSafeObjectInputStream;
                import java.io.ObjectInputStream;
                import java.io.InputStream;

                class Foo {
                    Acme readAcme(InputStream is) {
                        ObjectInputStream ois = createSafeObjectInputStream(is);
                        // read the obj
                        Acme acme = (Acme) ois.readObject();
                        return acme;
                    }
                }
                """);
  }
}
