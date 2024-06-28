package io.codemodder.remediation;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DefaultFixCandidateSearcherTest {

  private record Issue(String key, int line) {}

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

    List<Issue> allIssues = List.of(issue1, issue2, issue3);

    CompilationUnit cu = StaticJavaParser.parse(javaCode);
    DefaultFixCandidateSearcher<Issue> searcher =
        new DefaultFixCandidateSearcher<>("println", List.of());

    DetectorRule rule1 = new DetectorRule("key1", "rule 1", null);
    FixCandidateSearchResults<Issue> fixCandidateSearchResults =
        searcher.search(cu, "path", rule1, allIssues, Issue::key, Issue::line, i -> null);
    assertThat(fixCandidateSearchResults.unfixableFindings()).isEmpty();
    List<FixCandidate<Issue>> fixCandidates = fixCandidateSearchResults.fixCandidates();
    assertThat(fixCandidates).hasSize(2);

    // the first issue should match 2 issues and the first call which prints "Foo"
    FixCandidate<Issue> fixCandidate1 = fixCandidates.get(0);
    assertThat(fixCandidate1.methodCall().getArgument(0).toString()).hasToString("\"Foo\"");
    assertThat(fixCandidate1.issues()).containsExactly(issue1, issue2);

    // the second issue should match 1 issue and the second call which prints "Bar"
    FixCandidate<Issue> fixCandidate2 = fixCandidates.get(1);
    assertThat(fixCandidate2.methodCall().getArgument(0).toString()).hasToString("\"Bar\"");
    assertThat(fixCandidate2.issues()).containsExactly(issue3);
  }
}
