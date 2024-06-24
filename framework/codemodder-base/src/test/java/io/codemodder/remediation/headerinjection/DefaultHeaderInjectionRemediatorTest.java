package io.codemodder.remediation.headerinjection;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.DependencyGAV;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DefaultHeaderInjectionRemediatorTest {

  private DefaultHeaderInjectionRemediator remediator;
  private DetectorRule rule;

  @BeforeEach
  void setuo() {
    this.remediator = new DefaultHeaderInjectionRemediator();
    this.rule = new DetectorRule("header-injection", "Header Injection", null);
  }

  @Test
  void it_fixes_header_injection() {
    String vulnerableCode =
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
                """;

    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);

    HeaderInjectionFinding finding =
        new HeaderInjectionFinding("header-injection", "SearchController.java", 7);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu, "SearchController.java", rule, List.of(finding), f -> f.id, f -> f.line, f -> null);
    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(7);
    assertThat(change.getFixedFindings()).hasSize(1);
    FixedFinding fixedFinding = change.getFixedFindings().get(0);
    assertThat(fixedFinding.getRule()).isEqualTo(rule);
    assertThat(fixedFinding.getId()).isEqualTo("header-injection");

    assertThat(change.getDependenciesNeeded()).containsExactly(DependencyGAV.JAVA_SECURITY_TOOLKIT);

    String expectedFixedCode =
        """
                package com.acme;
                import io.github.pixee.security.Newlines;


                @RequestMapping("/search")
                public class SearchController extends BaseSearchController {
                    @GetMapping
                    public ResponseEntity<String> search(@RequestParam("q") String q) {
                        response.setHeader("X-Last-Search", Newlines.stripAll(q));
                        return ResponseEntity.ok(search());
                    }
                }
                """;

    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualTo(expectedFixedCode);
  }

  record HeaderInjectionFinding(String id, String path, int line) {}
}
