package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/** Strategy for remediating XXE vulnerabilities using Java's DOM parser. */
public interface XXEJavaRemediatorStrategy {

  /** A default implementation for callers. */
  XXEJavaRemediatorStrategy DEFAULT = new DefaultXXEJavaRemediatorStrategy();

  /** Remediate all XXE vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String string,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);
}