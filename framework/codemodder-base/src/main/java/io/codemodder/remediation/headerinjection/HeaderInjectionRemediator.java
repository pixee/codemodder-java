package io.codemodder.remediation.headerinjection;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/** Remediates header injection vulnerabilities. */
public interface HeaderInjectionRemediator {

  /** Remediate all header injection vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getStartLine,
      Function<T, Integer> getEndLine,
      Function<T, Integer> getColumn);

  /** The default header injection remediation strategy. */
  HeaderInjectionRemediator DEFAULT = new DefaultHeaderInjectionRemediator();
}
