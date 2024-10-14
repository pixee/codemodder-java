package io.codemodder.codemods.remediators.weakrandom;

import static io.codemodder.ast.ASTTransforms.addImportIfMissing;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import io.codemodder.CodemodChange;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import io.codemodder.codetf.FixedFinding;
import io.codemodder.codetf.UnfixedFinding;
import io.codemodder.remediation.RemediationMessages;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

final class DefaultWeakRandomRemediator implements WeakRandomRemediator {

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

    for (T issue : issuesForFile) {

      List<ObjectCreationExpr> unsafeRandoms =
          cu.findAll(ObjectCreationExpr.class).stream()
              .filter(oc -> oc.getType().asString().equals("Random"))
              .filter(oc -> getLine.apply(issue) == oc.getRange().get().begin.line)
              .filter(
                  oc -> {
                    Integer column = getColumn.apply(issue);
                    return column == null || column == oc.getRange().get().begin.column;
                  })
              .toList();

      if (unsafeRandoms.size() > 1) {
        unfixedFindings.add(
            new UnfixedFinding(
                getKey.apply(issue),
                detectorRule,
                path,
                getLine.apply(issue),
                RemediationMessages.multipleNodesFound));
        continue;
      } else if (unsafeRandoms.isEmpty()) {
        unfixedFindings.add(
            new UnfixedFinding(
                getKey.apply(issue),
                detectorRule,
                path,
                getLine.apply(issue),
                RemediationMessages.noNodesAtThatLocation));
        continue;
      }

      ObjectCreationExpr unsafeRandom = unsafeRandoms.get(0);
      unsafeRandom.setType("SecureRandom");
      addImportIfMissing(cu, SecureRandom.class.getName());
      changes.add(
          CodemodChange.from(
              getLine.apply(issue),
              List.of(),
              List.of(new FixedFinding(getKey.apply(issue), detectorRule))));
    }

    return CodemodFileScanningResult.from(changes, unfixedFindings);
  }
}
