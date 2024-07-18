package io.codemodder.remediation.headerinjection;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Remediates header injection vulnerabilities. */
public interface HeaderInjectionRemediator {

  /**
   * Remediate all header injection vulnerabilities in the given compilation unit.
   *
   * @param <T> the type of issue
   * @param cu the compilation unit being analyzed
   * @param path the path of the source file being analyzed
   * @param rule the detector rule for the issues
   * @param issuesForFile the issues to remediate
   * @param getKey strategy to retrieve the key for the issue from {@code T}
   * @param getLine strategy to retrieve the line for the issue described by {@code T}
   * @param getColumn strategy to retrieve the column of the line for the issue described by {@code
   *     T}, or {@code null} if the tool does not provide column information
   */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule rule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      ToIntFunction<T> getLine,
      ToIntFunction<T> getColumn);

  default <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      ToIntFunction<T> getLine) {
    return remediateAll(cu, path, detectorRule, issuesForFile, getKey, getLine, null);
  }

  /** The default header injection remediation strategy. */
  HeaderInjectionRemediator DEFAULT = new DefaultHeaderInjectionRemediator();
}
