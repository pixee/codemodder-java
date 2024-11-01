package io.codemodder.remediation;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.MethodCallExpr;
import io.codemodder.codetf.DetectorRule;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DefaultFixCandidateSearcherTest {

  private DefaultFixCandidateSearcher<Issue> searcher;
  private DetectorRule rule1;

  private record Issue(String key, int line) {}

  @BeforeEach
  void setup() {
    searcher =
        new DefaultFixCandidateSearcher<>(
            List.of(
                n ->
                    Optional.of(n)
                        .map(m -> m instanceof MethodCallExpr mce ? mce : null)
                        .filter(mce -> "println".equals(mce.getNameAsString()))
                        .isPresent()));
    rule1 = new DetectorRule("key1", "rule 1", null);
  }

  @Test
  void it_groups_overlapping_issues_together() {

    String javaCode =
        """
                        class A {
                            void has_issues_1_and_2() {
                                System.out.println("Foo"); // multiple issues point to this line
                            }

                            void has_issue_3() {
                                System.out.println("Bar");
                            }
                        }
                        """;

    Issue issue1 = new Issue("key1", 3);
    Issue issue2 = new Issue("key2", 3);
    Issue issue3 = new Issue("key3", 7);
    Issue issue4 = new Issue("key4", 505); // doesnt exist

    List<Issue> allIssues = new ArrayList<>(List.of(issue1, issue2, issue3, issue4));

    CompilationUnit cu = StaticJavaParser.parse(javaCode);

    FixCandidateSearchResults<Issue> fixCandidateSearchResults =
        searcher.search(
            cu,
            "path",
            rule1,
            allIssues,
            Issue::key,
            Issue::line,
            i -> Optional.empty(),
            i -> Optional.empty());
    List<FixCandidate<Issue>> legacyFixCandidates = fixCandidateSearchResults.fixCandidates();
    assertThat(legacyFixCandidates).hasSize(2);

    // There is no guarantee about issue ordering
    // Find if an issue with argument Foo exists
    Optional<FixCandidate<Issue>> maybeFooFixCandidateIssue =
        legacyFixCandidates.stream()
            .filter(
                fc ->
                    Optional.of(fc.node())
                        .map(n -> n instanceof MethodCallExpr mce ? mce : null)
                        .filter(
                            mce ->
                                mce.getArguments()
                                    .getFirst()
                                    .filter(arg -> "\"Foo\"".equals(arg.toString()))
                                    .isPresent())
                        .isPresent())
            .findAny();
    // Find if an issue with argument Bar exists
    Optional<FixCandidate<Issue>> maybeBarFixCandidateIssue =
        legacyFixCandidates.stream()
            .filter(
                fc ->
                    Optional.of(fc.node())
                        .map(n -> n instanceof MethodCallExpr mce ? mce : null)
                        .filter(
                            mce ->
                                mce.getArguments()
                                    .getFirst()
                                    .filter(arg -> "\"Bar\"".equals(arg.toString()))
                                    .isPresent())
                        .isPresent())
            .findAny();

    // one issue should match 2 issues and the first call which prints "Foo"
    assertThat(maybeFooFixCandidateIssue.isPresent()).isTrue();
    assertThat(maybeFooFixCandidateIssue.get().issues()).containsExactly(issue1, issue2);

    // another issue should match 1 issue and the second call which prints "Bar"
    assertThat(maybeBarFixCandidateIssue.isPresent()).isTrue();
    assertThat(maybeBarFixCandidateIssue.get().issues()).containsExactly(issue3);
  }

  @Test
  void it_finds_calls_that_span_multiple_lines() {

    String javaCode =
        """
                        class A {
                            void has_issues_1_and_2() {
                                int a = // both on line 3
                                  System.out.println("Foo"); // and on line 4
                            }
                        }
                        """;

    CompilationUnit cu = StaticJavaParser.parse(javaCode);

    Issue issueForLine3 = new Issue("key1", 3);
    Issue issueForLine4 = new Issue("key2", 4);
    List<Issue> issues = new ArrayList<>(List.of(issueForLine3, issueForLine4));

    // try without specifying an end line first.
    // the issue that points to startline=3 alone should not match the actual call location (4) so
    // it should not match
    // the issue that points to startline=4 alone should match the actual call location
    FixCandidateSearchResults<Issue> results =
        searcher.search(
            cu,
            "path",
            rule1,
            issues,
            Issue::key,
            Issue::line,
            i -> Optional.empty(),
            i -> Optional.empty());

    assertThat(results.fixCandidates().get(0).issues()).hasSize(1);
    assertThat(results.fixCandidates().get(0).issues()).containsExactly(issueForLine4);

    // now with the endline=4
    // the issue that points to startline=3 will match
    // the issue that points to startline=4 should still match
    results =
        searcher.search(
            cu,
            "path",
            rule1,
            issues,
            Issue::key,
            Issue::line,
            i -> Optional.of(4),
            i -> Optional.empty());

    assertThat(results.fixCandidates().get(0).issues()).hasSize(2);
    assertThat(results.fixCandidates().get(0).issues())
        .containsExactly(issueForLine3, issueForLine4);
  }
}
