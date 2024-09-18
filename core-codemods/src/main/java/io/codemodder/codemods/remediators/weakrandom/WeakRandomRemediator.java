package io.codemodder.codemods.remediators.weakrandom;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/** Fixes weak randomness. */
public interface WeakRandomRemediator {

  /** A default implementation for callers. */
  WeakRandomRemediator DEFAULT = new DefaultWeakRandomRemediator();

  /** Remediate all weak random vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);
}
