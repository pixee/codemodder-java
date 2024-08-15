package io.codemodder.remediation.headerinjection;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.RemediationMessages;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class DefaultHeaderInjectionRemediatorTest {

  private DefaultHeaderInjectionRemediator remediator;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    this.remediator = new DefaultHeaderInjectionRemediator();
    this.rule = new DetectorRule("header-injection", "Header Injection", null);
  }

  @Test
  void suggested_fix_works() {
    assertThat("Mascherano\nGerrard\rAlonso".replaceAll("[\r\n]", ""))
        .isEqualTo("MascheranoGerrardAlonso");
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_doesnt_fix_unfixable(final String unfixableCode, final int line, final String reason) {
    CompilationUnit cu = StaticJavaParser.parse(unfixableCode);
    LexicalPreservingPrinter.setup(cu);

    HeaderInjectionFinding finding =
        new HeaderInjectionFinding("header-injection", "SearchController.java", line);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "SearchController.java",
            rule,
            List.of(finding),
            f -> f.id,
            f -> line,
            f -> null,
            f -> null);
    assertThat(result.changes()).isEmpty();
    assertThat(result.unfixedFindings()).hasSize(1);
    UnfixedFinding unfixedFinding = result.unfixedFindings().get(0);
    assertThat(unfixedFinding.getReason()).isEqualTo(reason);
    assertThat(unfixedFinding.getLine()).isEqualTo(line);
    assertThat(unfixedFinding.getRule()).isEqualTo(rule);
  }

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        Arguments.of(
            """
                      package com.acme;
                      @Controller
                      public class SearchController {
                        @GetMapping
                        public ResponseEntity<String> search(@RequestParam("q") String q) {
                          response.header("X-Last-Search", q); // not a call we support
                          return ResponseEntity.ok(search());
                        }
                      }
                      """,
            6,
            RemediationMessages.noCallsAtThatLocation),
        Arguments.of(
            """
                        package com.acme;
                        @Controller
                        public class SearchController {
                          @GetMapping
                          public ResponseEntity<String> search(@RequestParam("q") String q) {
                            setHeader("X-Last-Search", q); // no scope -- should ignore
                            return ResponseEntity.ok(search());
                          }
                        }
                        """,
            6,
            RemediationMessages.noCallsAtThatLocation),
        Arguments.of(
            """
                          package com.acme;
                          @Controller
                          public class SearchController {
                            @GetMapping
                            public ResponseEntity<String> search(@RequestParam("q") String q) {
                              response.setHeader("X-Last-Search", "foo");
                              return ResponseEntity.ok(search());
                            }
                          }
                          """,
            6,
            RemediationMessages.noCallsAtThatLocation));
  }

  private static Stream<Arguments> fixableSamples() {
    return Stream.of(
        Arguments.of(
            """
                package com.acme;

                @RequestMapping("/search")
                public class SearchController extends BaseSearchController {
                    @GetMapping
                    public ResponseEntity<String> search(@RequestParam("q") String q) {
                        response.setHeader("X-Last-Search", q);
                        return ResponseEntity.ok(search());
                    }
                }
                """,
            7,
            """
                package com.acme;

                @RequestMapping("/search")
                public class SearchController extends BaseSearchController {
                    @GetMapping
                    public ResponseEntity<String> search(@RequestParam("q") String q) {
                        response.setHeader("X-Last-Search", stripNewlines(q));
                        return ResponseEntity.ok(search());
                    }

                    private static String stripNewlines(final String s) {
                        return s.replaceAll("[\\n\\r]", "");
                    }
                }
                """),
        Arguments.of(
            """
                      package com.acme;

                      @RequestMapping("/search")
                      public interface SearchController extends BaseSearchController {
                          @GetMapping
                          default ResponseEntity<String> search(@RequestParam("q") String q) {
                              response.setHeader("X-Last-Search", q);
                              return ResponseEntity.ok(search());
                          }
                      }
                      """,
            7,
            """
                package com.acme;

                @RequestMapping("/search")
                public interface SearchController extends BaseSearchController {
                    @GetMapping
                    default ResponseEntity<String> search(@RequestParam("q") String q) {
                        response.setHeader("X-Last-Search", q.replaceAll("[\\n\\r]", ""));
                        return ResponseEntity.ok(search());
                    }
                }
                """));
  }

  @ParameterizedTest
  @MethodSource("fixableSamples")
  void it_fixes_header_injection(
      final String vulnerableCode, final int line, final String expectedFixedCode) {

    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);

    HeaderInjectionFinding finding =
        new HeaderInjectionFinding("header-injection", "SearchController.java", line);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "SearchController.java",
            rule,
            List.of(finding),
            f -> f.id,
            f -> f.line,
            f -> null,
            f -> null);
    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(line);
    assertThat(change.getFixedFindings()).hasSize(1);
    FixedFinding fixedFinding = change.getFixedFindings().get(0);
    assertThat(fixedFinding.getRule()).isEqualTo(rule);
    assertThat(fixedFinding.getId()).isEqualTo("header-injection");

    assertThat(change.getDependenciesNeeded()).isEmpty();

    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualToIgnoringWhitespace(expectedFixedCode);
  }

  record HeaderInjectionFinding(String id, String path, int line) {}
}
