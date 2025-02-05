package io.codemodder;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.Remediator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** A mixin which provides basic structure for testing remediators. */
public interface RemediatorTestMixin<T> {

  @TestFactory
  default Stream<DynamicTest> it_fixes_the_code() {
    Stream<FixableSample> fixableSamples = createFixableSamples();

    return fixableSamples.map(
        sample -> {
          String beforeCode = sample.beforeCode();
          String afterCode = sample.afterCode();
          int line = sample.line();

          return DynamicTest.dynamicTest(
              beforeCode,
              () -> {
                CompilationUnit cu = StaticJavaParser.parse(beforeCode);
                LexicalPreservingPrinter.setup(cu);
                Remediator<Object> remediator = createRemediator();

                DetectorRule rule = new DetectorRule("rule-123", "my-remediation-rule", null);

                CodemodFileScanningResult result =
                    remediator.remediateAll(
                        cu,
                        "foo",
                        rule,
                        List.of(new Object()),
                        f -> "123",
                        f -> line,
                        x -> Optional.empty(),
                        x -> Optional.empty());
                assertThat(result.unfixedFindings()).isEmpty();
                assertThat(result.changes()).hasSize(1);
                CodemodChange change = result.changes().get(0);
                assertThat(change.lineNumber()).isEqualTo(line);

                String actualCode = LexicalPreservingPrinter.print(cu);
                assertThat(actualCode).isEqualToIgnoringCase(afterCode);
              });
        });
  }

  /** Build the remediator to test. */
  Remediator<Object> createRemediator();

  /** Create samples to test. */
  Stream<FixableSample> createFixableSamples();

  /** Create unfixable samples. */
  Stream<UnfixableSample> createUnfixableSamples();

  /** Represents a finding that can be fixed. */
  record FixableSample(String beforeCode, String afterCode, int line) {
    public FixableSample {
      Objects.requireNonNull(beforeCode);
      Objects.requireNonNull(afterCode);
      if (line < 0) {
        throw new IllegalArgumentException("Line number must be non-negative");
      }
    }
  }

  /** Represents a finding that can't be fixed for some reason. */
  record UnfixableSample(String code, int line) {
    public UnfixableSample {
      Objects.requireNonNull(code);
      if (line < 0) {
        throw new IllegalArgumentException("Line number must be non-negative");
      }
    }
  }
}
