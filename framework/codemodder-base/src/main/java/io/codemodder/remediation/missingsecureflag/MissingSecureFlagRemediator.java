package io.codemodder.remediation.missingsecureflag;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/** Strategy interface for remediating missing secure flag vulnerabilities. */
public interface MissingSecureFlagRemediator {

  /** Remediate all missing secure flag vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getStartLine,
      Function<T, Integer> getEndLine,
      Function<T, Integer> getStartColumn);

  MissingSecureFlagRemediator DEFAULT = new DefaultMissingSecureFlagRemediator();
}
