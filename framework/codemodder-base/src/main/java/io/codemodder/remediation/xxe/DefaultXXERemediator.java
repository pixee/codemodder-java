package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DefaultXXERemediator implements XXERemediator {

  private final List<XXEFixer> fixers;

  DefaultXXERemediator() {
    this.fixers =
        List.of(
            new DocumentBuilderFactoryAndSAXParserAtCreationFixer(),
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
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    List<CodemodChange> changes = new ArrayList<>();

    if(log.isDebugEnabled()) {
        String ids = issuesForFile.stream().map(getKey).collect(Collectors.joining(","));
        log.debug("Remediating {} issues ({}) for file {}", issuesForFile.size(), ids, path);
    }

    for (T issue : issuesForFile) {

      String findingId = getKey.apply(issue);
      int line = getLine.apply(issue);
      Integer column = getColumn.apply(issue);
      for (XXEFixer fixer : fixers) {
        log.debug("Trying {} to fix issue", fixer);
        XXEFixAttempt fixAttempt = fixer.tryFix(line, column, cu);
        log.debug("Fix attempt: responsible={} / success={}", fixAttempt.isResponsibleFixer(), fixAttempt.isFixed());
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

  private static final Logger log = LoggerFactory.getLogger(DefaultXXERemediator.class);
}
