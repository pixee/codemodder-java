package io.codemodder.remediation.reflectioninjection;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.DependencyGAV;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.remediation.RemediationMessages;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class DefaultReflectionInjectionRemediatorTest {

  private DefaultReflectionInjectionRemediator remediator;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    this.remediator = new DefaultReflectionInjectionRemediator();
    this.rule = new DetectorRule("reflection-injection", "Reflection Injection", null);
  }

  private static Stream<Arguments> fixableSamples() {
    return Stream.of(
        Arguments.of(
            """
                public class MyCode {
                  public void foo(String type) {
                        Class<?> clazz = Class.forName(type);
                        Object obj = clazz.newInstance();
                        Method method = clazz.getMethod("myMethod");
                        method.invoke(obj);
                  }
                }
                """,
            3,
            """
                import io.github.pixee.security.Reflection;

                public class MyCode {
                  public void foo(String type) {
                        Class<?> clazz = Reflection.loadAndVerify(type);
                        Object obj = clazz.newInstance();
                        Method method = clazz.getMethod("myMethod");
                        method.invoke(obj);
                  }
                }
                """),
        Arguments.of(
            """
                        import io.github.pixee.security.Reflection;

                        public class MyCode {
                          public void foo(String type) {
                                Class<?> clazz = Reflection.loadAndVerify(type);
                                Object obj = clazz.newInstance();
                                method.invoke(obj);
                                Class<?> clazz2 = Class.forName(type);
                          }
                        }
                        """,
            8,
            """
                        import io.github.pixee.security.Reflection;

                        public class MyCode {
                          public void foo(String type) {
                                Class<?> clazz = Reflection.loadAndVerify(type);
                                Object obj = clazz.newInstance();
                                method.invoke(obj);
                                Class<?> clazz2 = Reflection.loadAndVerify(type);
                          }
                        }
                        """),
        Arguments.of(
            """
                        import static io.github.pixee.security.Reflection.loadAndVerify;

                        public class MyCode {
                          public void foo(String type) {
                                Class<?> clazz = loadAndVerify(type);
                                Object obj = clazz.newInstance();
                                method.invoke(obj);
                                Class<?> clazz2 = Class.forName(type);
                          }
                        }
                        """,
            8,
            """
                        import io.github.pixee.security.Reflection;
                        import static io.github.pixee.security.Reflection.loadAndVerify;

                        public class MyCode {
                          public void foo(String type) {
                                Class<?> clazz = loadAndVerify(type);
                                Object obj = clazz.newInstance();
                                method.invoke(obj);
                                Class<?> clazz2 = Reflection.loadAndVerify(type);
                          }
                        }
                        """),
        Arguments.of(
            """
                        import static java.lang.Class.forName;

                        public class MyCode {
                          public void foo(String type) {
                                Class<?> clazz = forName(type);
                                Object obj = clazz.newInstance();
                                method.invoke(obj);
                          }
                        }
                        """,
            5,
            """
                        import io.github.pixee.security.Reflection;
                        import static java.lang.Class.forName;

                        public class MyCode {
                          public void foo(String type) {
                                Class<?> clazz = Reflection.loadAndVerify(type);
                                Object obj = clazz.newInstance();
                                method.invoke(obj);
                          }
                        }
                        """));
  }

  @ParameterizedTest
  @MethodSource("fixableSamples")
  void it_fixes_reflection_injection(
      final String vulnerableCode, final int line, final String expectedFixCode) {
    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);
    ReflectionInjectionFinding finding = new ReflectionInjectionFinding("id", line, null);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "foo",
            rule,
            List.of(finding),
            ReflectionInjectionFinding::key,
            ReflectionInjectionFinding::line,
            ReflectionInjectionFinding::column);
    assertThat(result.unfixedFindings()).isEmpty();
    assertThat(result.changes()).hasSize(1);

    assertThat(LexicalPreservingPrinter.print(cu)).isEqualTo(expectedFixCode);

    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(line);
    assertThat(change.getDependenciesNeeded()).containsExactly(DependencyGAV.JAVA_SECURITY_TOOLKIT);
    assertThat(change.getFixedFindings()).hasSize(1);
    assertThat(change.getFixedFindings().get(0).getId()).isEqualTo("id");
    assertThat(change.getFixedFindings().get(0).getRule()).isEqualTo(rule);
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_doesnt_fix_unfixables(final String unfixableCode, final int line, final String reason) {
    CompilationUnit cu = StaticJavaParser.parse(unfixableCode);
    LexicalPreservingPrinter.setup(cu);
    ReflectionInjectionFinding finding = new ReflectionInjectionFinding("id", line, null);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "foo",
            rule,
            List.of(finding),
            ReflectionInjectionFinding::key,
            ReflectionInjectionFinding::line,
            ReflectionInjectionFinding::column);
    assertThat(result.changes()).isEmpty();
    assertThat(result.unfixedFindings()).hasSize(1);
    assertThat(result.unfixedFindings().get(0).getLine()).isEqualTo(line);
    assertThat(result.unfixedFindings().get(0).getReason()).isEqualTo(reason);
    assertThat(result.unfixedFindings().get(0).getRule()).isEqualTo(rule);
    assertThat(result.unfixedFindings().get(0).getId()).isEqualTo("id");
  }

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        Arguments.of(
            """
                        public class MyCode {
                          public void foo(String type) {
                                Class<?> clazz = Class.byName(type); // unsupported method
                                Object obj = clazz.newInstance();
                                Method method = clazz.getMethod("myMethod");
                                method.invoke(obj);
                          }
                        }
                        """,
            3,
            RemediationMessages.noCallsAtThatLocation),
        Arguments.of(
            """
                        public class MyCode {
                          public void foo(String type) {
                                Class<?> clazz = Class.forName(type);Class clazz2 = Class.forName(type); // multiple supported methods
                                Object obj = clazz.newInstance();
                                Method method = clazz.getMethod("myMethod");
                                method.invoke(obj);
                          }
                        }
                        """,
            3,
            RemediationMessages.multipleCallsFound));
  }

  record ReflectionInjectionFinding(String key, int line, Integer column) {}
}
