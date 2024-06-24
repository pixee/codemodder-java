package io.codemodder.remediation.jndiinjection;

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

final class DefaultJNDIInjectionRemediatorTest {

  private DefaultJNDIInjectionRemediator remediator;
  private DetectorRule rule;

  @BeforeEach
  void setup() {
    this.remediator = new DefaultJNDIInjectionRemediator();
    this.rule = new DetectorRule("java/JNDIInjection", "JNDI Injection", null);
  }

  @ParameterizedTest
  @MethodSource("unfixableSamples")
  void it_doesnt_fix_unfixable(
      final String unfixableCode, final int line, final String failReason) {
    CompilationUnit cu = StaticJavaParser.parse(unfixableCode);
    LexicalPreservingPrinter.setup(cu);

    JNDIInjectionIssue issue = new JNDIInjectionIssue("key", line, null);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "JNDIUtil.java",
            rule,
            List.of(issue),
            JNDIInjectionIssue::key,
            JNDIInjectionIssue::line,
            JNDIInjectionIssue::column);

    assertThat(result.changes()).isEmpty();
    ;

    List<UnfixedFinding> unfixedFindings = result.unfixedFindings();
    assertThat(unfixedFindings).hasSize(1);
    UnfixedFinding unfixedFinding = unfixedFindings.get(0);
    assertThat(unfixedFinding.getId()).isEqualTo("key");
    assertThat(unfixedFinding.getLine()).isEqualTo(line);
    assertThat(unfixedFinding.getReason()).isEqualTo(failReason);
  }

  private static Stream<Arguments> unfixableSamples() {
    return Stream.of(
        Arguments.of("class Foo {}", 5, RemediationMessages.noCallsAtThatLocation),
        Arguments.of(
            """
                    class Foo {
                          void bar() {
                                String name = "foo";
                                Context ctx = new InitialContext();
                                Object obj1 = ctx.lookup(name); Object obj2 = ctx.lookup(name);
                            }
                    }
                    """,
            5,
            RemediationMessages.multipleCallsFound),
        Arguments.of(
            """
                class Foo {
                      static {
                            String name = "foo";
                            Context ctx = new InitialContext();
                            Object o = ctx.lookup(name);
                        }
                }
                """,
            5,
            "No method found around lookup call"));
  }

  @Test
  void it_fixes_jndi_injection() {
    String vulnerableCode =
        """
                import javax.naming.Context;
                import javax.naming.InitialContext;
                import javax.http.servlet.HttpServletRequest;

                public class JNDIUtil {
                    public static void lookup(HttpServletRequest request) {
                        String name = request.getParameter("name");
                        Context ctx = new InitialContext();
                        Object obj = ctx.lookup(name);
                    }
                }
                """;

    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);

    JNDIInjectionIssue issue = new JNDIInjectionIssue("key", 9, null);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "JNDIUtil.java",
            rule,
            List.of(issue),
            JNDIInjectionIssue::key,
            JNDIInjectionIssue::line,
            JNDIInjectionIssue::column);

    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(9);

    List<FixedFinding> fixedFindings = change.getFixedFindings();
    assertThat(fixedFindings).hasSize(1);
    FixedFinding fixedFinding = fixedFindings.get(0);
    assertThat(fixedFinding.getId()).isEqualTo("key");
    assertThat(fixedFinding.getRule()).isEqualTo(rule);

    String fixedCode =
        """
                import java.util.Set;
                import javax.naming.Context;
                import javax.naming.InitialContext;
                import javax.http.servlet.HttpServletRequest;

                public class JNDIUtil {
                    public static void lookup(HttpServletRequest request) {
                        String name = request.getParameter("name");
                        Context ctx = new InitialContext();
                        validateResourceName(name);
                        Object obj = ctx.lookup(name);
                    }

                    private static void validateResourceName(final String name) {
                        if (name != null) {
                            Set<String> illegalNames = Set.of("ldap://", "rmi://", "dns://", "java:");
                            String canonicalName = name.toLowerCase().trim();
                            if (illegalNames.stream().anyMatch(canonicalName::startsWith)) {
                                throw new SecurityException("Illegal JNDI resource name: " + name);
                            }
                        }
                    }
                }
                """;

    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(fixedCode).isEqualToIgnoringWhitespace(actualCode);
  }

  @Test
  void it_fixes_without_duplicating_control_method() {
    String vulnerableCode =
        """
                import java.util.Set;
                import javax.naming.Context;
                import javax.naming.InitialContext;
                import javax.http.servlet.HttpServletRequest;

                public class JNDIUtil {
                    public static void lookup(HttpServletRequest request) {
                        String name = request.getParameter("name");
                        Context ctx = new InitialContext();
                        Object obj = ctx.lookup(name);
                    }

                    private static void validateResourceName(final String name) {
                        if (name != null) {
                            Set<String> illegalNames = Set.of("ldap://", "rmi://", "dns://", "java:");
                            String canonicalName = name.toLowerCase().trim();
                            if (illegalNames.stream().anyMatch(canonicalName::startsWith)) {
                                throw new SecurityException("Illegal JNDI resource name: " + name);
                            }
                        }
                    }
                }
                """;

    CompilationUnit cu = StaticJavaParser.parse(vulnerableCode);
    LexicalPreservingPrinter.setup(cu);

    JNDIInjectionIssue issue = new JNDIInjectionIssue("key", 10, null);
    CodemodFileScanningResult result =
        remediator.remediateAll(
            cu,
            "JNDIUtil.java",
            rule,
            List.of(issue),
            JNDIInjectionIssue::key,
            JNDIInjectionIssue::line,
            JNDIInjectionIssue::column);

    assertThat(result.changes()).hasSize(1);
    CodemodChange change = result.changes().get(0);
    assertThat(change.lineNumber()).isEqualTo(10);

    List<FixedFinding> fixedFindings = change.getFixedFindings();
    assertThat(fixedFindings).hasSize(1);
    FixedFinding fixedFinding = fixedFindings.get(0);
    assertThat(fixedFinding.getId()).isEqualTo("key");
    assertThat(fixedFinding.getRule()).isEqualTo(rule);

    String fixedCode =
        """
                import java.util.Set;
                import javax.naming.Context;
                import javax.naming.InitialContext;
                import javax.http.servlet.HttpServletRequest;

                public class JNDIUtil {
                    public static void lookup(HttpServletRequest request) {
                        String name = request.getParameter("name");
                        Context ctx = new InitialContext();
                        validateResourceName(name);
                        Object obj = ctx.lookup(name);
                    }

                    private static void validateResourceName(final String name) {
                        if (name != null) {
                            Set<String> illegalNames = Set.of("ldap://", "rmi://", "dns://", "java:");
                            String canonicalName = name.toLowerCase().trim();
                            if (illegalNames.stream().anyMatch(canonicalName::startsWith)) {
                                throw new SecurityException("Illegal JNDI resource name: " + name);
                            }
                        }
                    }
                }
                """;

    String actualCode = LexicalPreservingPrinter.print(cu);
    assertThat(fixedCode).isEqualToIgnoringWhitespace(actualCode);
  }

  record JNDIInjectionIssue(String key, int line, Integer column) {}
}
