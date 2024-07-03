package io.codemodder.remediation.xss;

import static java.util.stream.Collectors.groupingBy;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

final class DefaultXSSRemediator implements XSSRemediator {

  private final List<XSSCodeShapeFixer> javaFixers;

  DefaultXSSRemediator() {
    this.javaFixers = List.of(new PrintingMethodFixer(), new NakedVariableReturnFixer());
  }

  @Override
  public <T> CodemodFileScanningResult remediateJava(
      final CompilationUnit cu,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {

    List<XSSFixGroup<T>> fixGroups = createFixGroups(issuesForFile, getLine, getColumn);

    List<UnfixedFinding> unfixedFindings = new ArrayList<>();
    List<CodemodChange> changes = new ArrayList<>();

    for (XSSFixGroup<T> fixGroup : fixGroups) {
      boolean foundResponsibleFixer = false;
      for (XSSCodeShapeFixer javaFixer : javaFixers) {
        foundResponsibleFixer = true;
        XSSCodeShapeFixResult fixResult =
            javaFixer.fixCodeShape(
                cu, path, detectorRule, fixGroup.issues(), getKey, getLine, getColumn);
        if (fixResult.isResponsibleFixer()) {
          if (fixResult.isFixed()) {
            List<FixedFinding> fixes =
                fixGroup.issues().stream()
                    .map(fix -> new FixedFinding(getKey.apply(fix), detectorRule))
                    .toList();
            changes.add(CodemodChange.from(fixResult.line(), List.of(), fixes));
          } else {
            List<UnfixedFinding> unfixed =
                fixGroup.issues().stream()
                    .map(
                        fix ->
                            new UnfixedFinding(
                                getKey.apply(fix),
                                detectorRule,
                                path,
                                fixResult.line(),
                                fixResult.reasonNotFixed()))
                    .toList();
            unfixedFindings.addAll(unfixed);
          }
          // if this was the responsible fixer, no need to continue
          break;
        }
      }
      if (!foundResponsibleFixer) {
        unfixedFindings.addAll(
            fixGroup.issues().stream()
                .map(
                    fix ->
                        new UnfixedFinding(
                            getKey.apply(fix),
                            detectorRule,
                            path,
                            getLine.apply(fix),
                            "Couldn't fix that shape of code"))
                .toList());
      }
    }
    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }

  /** Split the issues into fix groups that all have the same location. */
  private <T> List<XSSFixGroup<T>> createFixGroups(
      final List<T> issuesForFile,
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {
    List<XSSFixGroup<T>> fixGroups = new ArrayList<>();

    Map<Integer, List<T>> fixesPerLine = issuesForFile.stream().collect(groupingBy(getLine));

    // now further separate the fixes by column if it's available
    for (Map.Entry<Integer, List<T>> entry : fixesPerLine.entrySet()) {
      Map<Integer, List<T>> fixesPerColumn =
          entry.getValue().stream().collect(groupingBy(getColumn));
      for (List<T> columnFixes : fixesPerColumn.values()) {
        fixGroups.add(new XSSFixGroup<>(columnFixes));
      }
    }

    return List.copyOf(fixGroups);
  }

  @Override
  public <T> CodemodFileScanningResult remediateJSP(
      final Path filePath,
      final String path,
      final DetectorRule detectorRule,
      final List<T> issuesForFile,
      final Function<T, String> getKey,
      final Function<T, Integer> getLine,
      final Function<T, Integer> getColumn) {
    return CodemodFileScanningResult.none();
  }
}
