package io.codemodder.remediation.ssrf;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/** Fixes SSRF vulnerabilities. */
public interface SSRFRemediator {

  /** A default implementation for callers. */
  SSRFRemediator DEFAULT = new DefaultSSRFRemediator();

  /** Remediate all SSRF vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getStartLine,
      Function<T, Integer> getEndLine,
      Function<T, Integer> getColumn);
}
