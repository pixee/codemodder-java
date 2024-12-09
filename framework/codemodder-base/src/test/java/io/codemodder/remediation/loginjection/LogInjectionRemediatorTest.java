package io.codemodder.remediation.loginjection;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.javaparser.JavaParserFactory;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class LogInjectionRemediatorTest {

  private DetectorRule rule;
  private JavaParser parser;

  @BeforeEach
  void setup() throws IOException {
    this.parser = JavaParserFactory.newFactory().create(List.of());
    this.rule = new DetectorRule("log-injection", "Log Injection", null);
  }

  private static Stream<Arguments> fixableSamples() {
    return Stream.of(
        Arguments.of(
            """
                    class Foo {
                      void foo(String msg) {
                         log.info(msg);
                      }
                    }
                    """,
            """
                    import static io.github.pixee.security.Newlines.stripNewLines;
                    class Foo {
                      void foo(String msg) {
                        log.info(stripNewLines(msg));
                      }
                    }
                    """,
            3),
        Arguments.of(
            """
                            class Foo {
                              void foo(String msg, MyException ex) {
                                 log.info(msg, ex);
                              }
                            }
                            """,
            """
                        import static io.github.pixee.security.Newlines.stripNewLines;

                        class Foo {
                          void foo(String msg, MyException ex) {
                            log.info(stripNewLines(msg), ex);
                          }
                        }
                        """,
            3),
        Arguments.of(
            """
                            class Foo {
                              void foo(String msg) {
                                 MyException ex = null;
                                 log.info(msg, ex);
                              }
                            }
                            """,
            """
                        import static io.github.pixee.security.Newlines.stripNewLines;

                        class Foo {
                          void foo(String msg) {
                            MyException ex = null;
                            log.info(stripNewLines(msg), ex);
                          }
                        }
                        """,
            4),
        Arguments.of(
            """
                            class Foo {
                              void foo(String msg) {
                                try {
                                  doThing();
                                } catch(MyException e) {
                                  log.info(msg, e);
                                }
                              }
                            }
                            """,
            """
                        import static io.github.pixee.security.Newlines.stripNewLines;

                        class Foo {
                              void foo(String msg) {
                                try {
                                  doThing();
                                } catch(MyException e) {
                                  log.info(stripNewLines(msg), e);
                                }
                              }
                            }
                        """,
            6),
        Arguments.of(
            """
                            class Foo {
                              void foo(String user, MyException ex) {
                                 log.info("hi " + user, ex);
                              }
                            }
                            """,
            """
                        import static io.github.pixee.security.Newlines.stripNewLines;

                        class Foo {
                          void foo(String user, MyException ex) {
                            log.info("hi " + stripNewLines(user), ex);
                          }
                        }
                        """,
            3),
        Arguments.of(
            """
                            class Foo {
                              void foo(String user, MyException ex) {
                                 log.info("hi " + user + " its me!", ex);
                              }
                            }
                            """,
            """
                        import static io.github.pixee.security.Newlines.stripNewLines;

                        class Foo {
                          void foo(String user, MyException ex) {
                            log.info("hi " + stripNewLines(user) + " its me!", ex);
                          }
                        }
                        """,
            3),
        Arguments.of(
            """
                            class Foo {
                              void foo(String user, MyException ex) {
                                 log.info("hi {}", user, ex);
                              }
                            }
                            """,
            """
                        import static io.github.pixee.security.Newlines.stripNewLines;

                        class Foo {
                          void foo(String user, MyException ex) {
                            log.info("hi {}", stripNewLines(user), ex);
                          }
                        }
                        """,
            3));
  }

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        Arguments.of(
            """
                    class Foo {
                      void foo(String msg) {
                         log.weirdMethodName("hi " + msg); // method name is not a log method
                      }
                    }
                    """,
            3),
        Arguments.of(
            """
                            class Foo {
                              void foo(String s1, String s2) {
                                 log.debug("hi " + s1 + " and " + s2); // not sure what to fix
                              }
                            }
                            """,
            3),
        Arguments.of(
            """
                            class Foo {
                              void foo(String s1) {
                                 debug("hi " + s1); // no scope
                              }
                            }
                            """,
            3));
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_does_not_fix_samples(final String beforeCode, final int line) {
    CompilationUnit cu = parser.parse(beforeCode).getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);
    var result = scanAndFix(cu, line);
    assertThat(result.changes()).isEmpty();
  }

  @ParameterizedTest
  @MethodSource("fixableSamples")
  void it_fixes_samples(final String beforeCode, final String afterCode, final int line) {
    CompilationUnit cu = parser.parse(beforeCode).getResult().orElseThrow();
    LexicalPreservingPrinter.setup(cu);

    var result = scanAndFix(cu, line);

    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(line);

    List<FixedFinding> fixedFindings = change.getFixedFindings();
    assertThat(fixedFindings).hasSize(1);
    FixedFinding fixedFinding = fixedFindings.get(0);
    assertThat(fixedFinding.getId()).isEqualTo("key");
    assertThat(fixedFinding.getRule()).isEqualTo(rule);
    assertThat(result.changes()).isNotEmpty();
    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(actualCode).isEqualToIgnoringWhitespace(afterCode);
  }

  private CodemodFileScanningResult scanAndFix(final CompilationUnit cu, final int line) {
    LogInjectionRemediator<Object> remediator = new LogInjectionRemediator<>();
    return remediator.remediateAll(
        cu,
        "Log.java",
        rule,
        List.of(new Object()),
        f -> "key",
        f -> line,
        i -> Optional.empty(),
        i -> Optional.empty());
  }
}
