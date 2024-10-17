package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class DefaultXXERemediator implements XXERemediator {

  private final List<XXEFixer> fixers;

  DefaultXXERemediator() {
    this.fixers =
        List.of(
            new DocumentBuilderFactoryAndSAXParserAtCreationFixer(),
            new DocumentBuilderFactoryAtNewDBFixer(),
            new SAXParserAtNewSPFixer(),
            new DocumentBuilderFactoryAtParseFixer(),
            new TransformerFactoryAtCreationFixer(),
            new XMLReaderAtParseFixer());
  }

  @Override
  public <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getStartLine,
      final Function<T, Integer> getStartColumn) {

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    List<CodemodChange> changes = new ArrayList<>();

    for (T issue : issuesForFile) {

      String findingId = getKey.apply(issue);
      int line = getStartLine.apply(issue);
      Integer column = getStartColumn.apply(issue);
      for (XXEFixer fixer : fixers) {
        XXEFixAttempt fixAttempt = fixer.tryFix(line, column, cu);
        if (!fixAttempt.isResponsibleFixer()) {
          continue;
        }
        if (fixAttempt.isFixed()) {
          CodemodChange change =
              CodemodChange.from(line, new FixedFinding(findingId, detectorRule));
          changes.add(change);
        } else {
          UnfixedFinding unfixedFinding =
              new UnfixedFinding(findingId, detectorRule, path, line, fixAttempt.reasonNotFixed());
          unfixedFindings.add(unfixedFinding);
        }
      }
    }

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }
}
