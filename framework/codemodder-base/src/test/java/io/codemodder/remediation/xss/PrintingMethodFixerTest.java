package io.codemodder.remediation.xss;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class PrintingMethodFixerTest {

  private PrintingMethodFixer fixer;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    this.fixer = new PrintingMethodFixer();
    this.rule = new DetectorRule("xss", "XSS", null);
  }

  private static Stream<Arguments> fixableSamples() {
    return Stream.of(
        Arguments.of(
            """
            class Samples {
              void should_be_fixed(String s) {
                print(s);
              }
            }
            """,
            """
                import org.owasp.encoder.Encode;
                class Samples {
                  void should_be_fixed(String s) {
                    print(Encode.forHtml(s));
                  }
                }
                """),
        Arguments.of(
            """
           class Samples {
             void should_be_fixed(String s) {
               write(s);
             }
           }
           """,
            """
           import org.owasp.encoder.Encode;
           class Samples {
             void should_be_fixed(String s) {
               write(Encode.forHtml(s));
             }
           }
           """),
        Arguments.of(
            """
                        class Samples {
                          void should_be_fixed(String s) {
                            getWriter().write(s);
                          }
                        }
                        """,
            """
                        import org.owasp.encoder.Encode;
                        class Samples {
                          void should_be_fixed(String s) {
                            getWriter().write(Encode.forHtml(s));
                          }
                        }
                        """));
  }

  @ParameterizedTest
  @MethodSource("fixableSamples")
  void it_fixes_obvious_response_write_methods(final String beforeCode, final String afterCode) {
    CompilationUnit cu = StaticJavaParser.parse(beforeCode);
    LexicalPreservingPrinter.setup(cu);

    XSSFinding finding = new XSSFinding("should_be_fixed", 3, null);
    XSSCodeShapeFixResult result =
        fixer.fixCodeShape(
            cu,
            "path",
            rule,
            List.of(finding),
            XSSFinding::key,
            XSSFinding::line,
            XSSFinding::column);
    assertThat(result.isResponsibleFixer()).isTrue();
    assertThat(result.isFixed()).isTrue();
    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualToIgnoringWhitespace(afterCode);
  }
}
