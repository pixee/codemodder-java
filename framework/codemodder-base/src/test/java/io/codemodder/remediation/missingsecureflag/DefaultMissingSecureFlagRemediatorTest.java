package io.codemodder.remediation.missingsecureflag;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class DefaultMissingSecureFlagRemediatorTest {

  private MissingSecureFlagRemediator<Object> remediator;

  @BeforeEach
  void setup() {
    remediator = new MissingSecureFlagRemediator<>();
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
            package com.acme.web;
            class MyServlet extends HttpServlet {
                void doGet(HttpServletRequest request, HttpServletResponse response) {
                    Cookie cookie = new Cookie("name", "value");
                    addCookie(cookie); // no scope
                }
            }
            """,
        """
            package com.acme.web;
            class MyServlet extends HttpServlet {
                void doGet(HttpServletRequest request, HttpServletResponse response) {
                    Cookie cookie = new Cookie("name", "value");
                    response.addCookie(); // no args
                }
            }
            """,
        """
            package com.acme.web;
            class MyServlet extends HttpServlet {
                void doGet(HttpServletRequest request, HttpServletResponse response) {
                    Cookie cookie = new Cookie("name", "value");
                    int a = 1;
                    response.addCookie(cookie); // wrong line
                }
            }
            """
      })
  void it_does_not_add_secure_flag(final String javaCode) {
    CompilationUnit cu = StaticJavaParser.parse(javaCode);
    LexicalPreservingPrinter.setup(cu);
    DetectorRule rule = new DetectorRule("insecure-cookie", "Add secure flag", null);

    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "MyServlet.java",
            rule,
            List.of(new Object()),
            r -> "id-1",
            r -> 5,
            r -> Optional.empty(),
            r -> Optional.empty());

    assertThat(result.changes()).isEmpty();
    result
        .unfixedFindings()
        .forEach(unfixedFinding -> assertThat(unfixedFinding.getReason()).isNotEmpty());
  }

  /**
   * This test should be able to pass with "5" as well, ideally. Unfortunately, this can cause
   * confusion and result in a "double fix".
   */
  @ParameterizedTest
  @ValueSource(ints = {6})
  void it_adds_secure_flag(final int line) {
    String javaCode =
        """
                package com.acme.web;

                class MyServlet extends HttpServlet {
                    void doGet(HttpServletRequest request, HttpServletResponse response) {
                        Cookie cookie = new Cookie("name", "value");
                        response.addCookie(cookie);
                    }
                }
                """;

    CompilationUnit cu = StaticJavaParser.parse(javaCode);
    LexicalPreservingPrinter.setup(cu);
    DetectorRule rule = new DetectorRule("insecure-cookie", "Add secure flag", null);

    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "MyServlet.java",
            rule,
            List.of(new Object()),
            r -> "id-1",
            r -> line,
            r -> Optional.empty(),
            r -> Optional.empty());

    assertThat(result.unfixedFindings()).isEmpty();
    List<CodemodChange> changes = result.changes();
    assertThat(changes).hasSize(1);
    CodemodChange change = changes.get(0);

    assertThat(change.getDependenciesNeeded()).isEmpty();
    assertThat(change.lineNumber()).isEqualTo(line);
    List<FixedFinding> fixedFindings = change.getFixedFindings();
    assertThat(fixedFindings).hasSize(1);

    FixedFinding fixedFinding = fixedFindings.get(0);
    assertThat(fixedFinding.getId()).isEqualTo("id-1");
    assertThat(fixedFinding.getRule()).isSameAs(rule);

    String actual = LexicalPreservingPrinter.print(cu);

    String fixedCode =
        """
                package com.acme.web;

                class MyServlet extends HttpServlet {
                    void doGet(HttpServletRequest request, HttpServletResponse response) {
                        Cookie cookie = new Cookie("name", "value");
                        cookie.setSecure(true);
                        response.addCookie(cookie);
                    }
                }
                """;

    assertThat(actual).isEqualToIgnoringWhitespace(fixedCode);
  }
}
