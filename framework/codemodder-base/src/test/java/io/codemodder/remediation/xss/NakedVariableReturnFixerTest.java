package io.codemodder.remediation.xss;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.FixCandidateSearcher;
import io.codemodder.remediation.RemediationMessages;
import io.codemodder.remediation.SearcherStrategyRemediator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class NakedVariableReturnFixerTest {

  private DetectorRule detectorRule;

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        Arguments.of(
            """
            class Samples {
              String foo = null;
              Map map = null;
              void should_not_be_fixed_method_call() {
                return unknownMethodCall(foo);
              }
            }
            """,
            5,
            RemediationMessages.noNodesAtThatLocation),
        Arguments.of(
            """
            class Samples {
              String foo = null;
              Map map = null;
              void should_not_be_fixed_multiple_returns() {
                return foo; return map;
              }
            }
            """,
            5,
            RemediationMessages.multipleNodesFound),
        Arguments.of(
            """
                class Samples {
                  String foo = null;
                  Map map = null;
                  void should_not_be_fixed_multiple_returns() {
                    write(foo);
                  }
                }
                """,
            5,
            RemediationMessages.noNodesAtThatLocation));
  }

  @BeforeEach
  void setup() {
    detectorRule = new DetectorRule("xss", "XSS", null);
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_does_not_fix_unexpected_shapes(
      final String code, final int line, final String reasonForFailure) {
    CompilationUnit cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);

    var fixer = new NakedVariableReturnFixStrategy();

    List<XSSFinding> findings = List.of(new XSSFinding("id", line, null));

    var remediator =
        new SearcherStrategyRemediator.Builder<XSSFinding>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<XSSFinding>()
                    .withMatcher(NakedVariableReturnFixStrategy::match)
                    .build(),
                fixer)
            .build();
    var result =
        remediator.remediateAll(
            cu,
            "path",
            detectorRule,
            findings,
            XSSFinding::key,
            XSSFinding::line,
            x -> Optional.empty(),
            x -> Optional.ofNullable(x.column()));
    assertThat(result.unfixedFindings().isEmpty()).isFalse();
    assertThat(result.unfixedFindings().get(0).getReason()).isEqualTo(reasonForFailure);
  }

  @Test
  void it_fixes_expected_shapes() {
    String code =
        """
                class Samples {
                  String foo = null;
                  Map map = null;
                  void should_be_fixed_simple_name() {
                    return foo;
                  }
                }
                """;
    CompilationUnit cu = StaticJavaParser.parse(code);
    LexicalPreservingPrinter.setup(cu);

    var fixer = new NakedVariableReturnFixStrategy();

    // when we try to find both, we should error - fix groups should only have one location
    List<XSSFinding> findings = List.of(new XSSFinding("should_be_fixed_simple_name", 5, null));

    var remediator =
        new SearcherStrategyRemediator.Builder<XSSFinding>()
            .withSearcherStrategyPair(
                new FixCandidateSearcher.Builder<XSSFinding>()
                    .withMatcher(NakedVariableReturnFixStrategy::match)
                    .build(),
                fixer)
            .build();
    var result =
        remediator.remediateAll(
            cu,
            "path",
            detectorRule,
            findings,
            XSSFinding::key,
            XSSFinding::line,
            x -> Optional.empty(),
            x -> Optional.ofNullable(x.column()));
    assertThat(result.changes().isEmpty()).isFalse();
    assertThat(result.changes().get(0).lineNumber()).isEqualTo(5);

    String afterCode = LexicalPreservingPrinter.print(cu);
    assertThat(afterCode)
        .isEqualToIgnoringWhitespace(
            """
                  import org.owasp.encoder.Encode;

                  class Samples {
                      String foo = null;
                      Map map = null;
                      void should_be_fixed_simple_name() {
                        return Encode.forHtml(foo);
                      }
                  }
                  """);
  }
}
