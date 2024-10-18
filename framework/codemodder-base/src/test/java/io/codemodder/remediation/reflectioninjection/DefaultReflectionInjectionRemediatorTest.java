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
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link ReflectionInjectionRemediator} that use a fake detection input type. */
final class DefaultReflectionInjectionRemediatorTest {

  private ReflectionInjectionRemediator<ReflectionInjectionFinding> remediator;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    this.remediator = new ReflectionInjectionRemediator<>();
    this.rule = new DetectorRule("reflection-injection", "Reflection Injection", null);
  }

  @Nested
  final class Fixable {

    @Test
    void fix_simple() {
      verifyFixable(
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
              """);
    }

    @Test
    void fix_second_instance() {
      verifyFixable(
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
                          """);
    }

    @Test
    void fix_second_instance_static_import_utility() {
      verifyFixable(
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
                          """);
    }

    /** Edge case where the user has static imported {@link Class#forName(String)}. */
    @Test
    void fix_static_import_for_name() {
      verifyFixable(
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
                          """);
    }

    /**
     * An edge case where the user has specified the fully qualified class name. No one would ever
     * do this, because java.lang is always imported. But if they do, we'll fix it.
     *
     * <p>This test does not pass. We would need to mess with Java symbol solving configuration for
     * the parser to get it to recognize that {@code java.lang} is a package. This is not worth the
     * effort for this edge case.
     */
    @Disabled(
        "JavaParser symbol resolution configuration identifies java.lang as a field expression")
    @Test
    void fix_fully_qualified_class_for_name() {
      verifyFixable(
          """
            public class MyCode {
              public void foo(String type) {
                    Class<?> clazz = java.lang.Class.forName(type);
                    Object obj = clazz.newInstance();
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
                                  method.invoke(obj);
                            }
                          }
                          """);
    }

    private void verifyFixable(
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
              f -> Optional.empty(),
              f -> Optional.ofNullable(f.column));
      assertThat(result.unfixedFindings()).isEmpty();
      assertThat(result.changes()).hasSize(1);

      assertThat(LexicalPreservingPrinter.print(cu)).isEqualTo(expectedFixCode);

      CodemodChange change = result.changes().get(0);
      assertThat(change.lineNumber()).isEqualTo(line);
      assertThat(change.getDependenciesNeeded())
          .containsExactly(DependencyGAV.JAVA_SECURITY_TOOLKIT);
      assertThat(change.getFixedFindings()).hasSize(1);
      assertThat(change.getFixedFindings().get(0).getId()).isEqualTo("id");
      assertThat(change.getFixedFindings().get(0).getRule()).isEqualTo(rule);
    }
  }

  @Nested
  final class Unfixable {

    @Test
    void unfixable_because_no_calls_at_specified_location() {
      verifyUnfixable(
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
          RemediationMessages.noNodesAtThatLocation);
    }

    @Test
    void unfixable_because_multiple_calls_found_on_line_and_no_column_provided() {
      verifyUnfixable(
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
          RemediationMessages.multipleNodesFound);
    }

    private void verifyUnfixable(
        final String unfixableCode,
        @SuppressWarnings("SameParameterValue") final int line,
        final String reason) {
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
              f -> Optional.empty(),
              f -> Optional.ofNullable(f.column));
      assertThat(result.changes()).isEmpty();
      assertThat(result.unfixedFindings()).hasSize(1);
      assertThat(result.unfixedFindings().get(0).getLine()).isEqualTo(line);
      assertThat(result.unfixedFindings().get(0).getReason()).isEqualTo(reason);
      assertThat(result.unfixedFindings().get(0).getRule()).isEqualTo(rule);
      assertThat(result.unfixedFindings().get(0).getId()).isEqualTo("id");
    }
  }

  record ReflectionInjectionFinding(String key, int line, Integer column) {}
}
