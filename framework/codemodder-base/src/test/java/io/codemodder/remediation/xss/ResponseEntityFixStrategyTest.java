package io.codemodder.remediation.xss;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.javaparser.JavaParserFactory;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.SearcherStrategyRemediator;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class ResponseEntityFixStrategyTest {

  private ResponseEntityFixStrategy fixer;
  private DetectorRule rule;
  private JavaParser parser;

  @BeforeEach
  void setup() throws IOException {
    this.fixer = new ResponseEntityFixStrategy();
    this.parser = JavaParserFactory.newFactory().create(List.of());
    this.rule = new DetectorRule("xss", "XSS", null);
  }

  private static Stream<Arguments> fixableSamples() {
    return Stream.of(
        Arguments.of(
            """
            class Samples {
              String should_be_fixed(String s) {
                return new ResponseEntity(s, HttpStatus.OK);
              }
            }
            """,
            """
                import org.owasp.encoder.Encode;
                class Samples {
                  String should_be_fixed(String s) {
                    return new ResponseEntity(Encode.forHtml(s), HttpStatus.OK);
                  }
                }
                """),
        Arguments.of(
            """
           class Samples {
             ResponseEntity<String> should_be_fixed(String s) {
               return new ResponseEntity<>(s + "hi", HttpStatus.OK);
             }
           }
           """,
            """
           import org.owasp.encoder.Encode;
           class Samples {
             ResponseEntity<String> should_be_fixed(String s) {
               return new ResponseEntity<>(Encode.forHtml(s) + "hi", HttpStatus.OK);
             }
           }
           """));
  }

  @ParameterizedTest
  @MethodSource("fixableSamples")
  void it_fixes_obvious_response_write_methods(final String beforeCode, final String afterCode) {
    CompilationUnit cu = parser.parse(beforeCode).getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);

    var result = scanAndFix(cu, 3);
    assertThat(result.changes()).isNotEmpty();
    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualToIgnoringWhitespace(afterCode);
  }

  private CodemodFileScanningResult scanAndFix(final CompilationUnit cu, final int line) {
    XSSFinding finding = new XSSFinding("should_be_fixed", line, null);
    var remediator =
        new SearcherStrategyRemediator.Builder<XSSFinding>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<XSSFinding>()
                    .withMatcher(ResponseEntityFixStrategy::match)
                    .build(),
                fixer)
            .build();
    return remediator.remediateAll(
        cu,
        "path",
        rule,
        List.of(finding),
        XSSFinding::key,
        XSSFinding::line,
        x -> Optional.empty(),
        x -> Optional.ofNullable(x.column()));
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_does_not_fix_unfixable_samples(final String beforeCode, final int line) {
    CompilationUnit cu = parser.parse(beforeCode).getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);
    var result = scanAndFix(cu, line);
    assertThat(result.changes()).isEmpty();
  }

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        // this is not a ResponseEntity, shouldn't touch it
        Arguments.of(
            // this is not a ResponseEntity, shouldn't touch it
            """
                        class Samples {
                          String should_not_be_fixed(String s) {
                            return new NotResponseEntity(s, HttpStatus.OK);
                          }
                        }
                        """,
            3),
        Arguments.of(
            // this is not a String, shouldn't touch it
            """
                            class Samples {
                              String should_not_be_fixed(BodyType s) {
                                return new ResponseEntity(s, HttpStatus.OK);
                              }
                            }
                            """,
            3));
  }
}
