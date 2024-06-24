package io.codemodder.remediation.reflectioninjection;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/** Remediates reflection injection vulnerabilities. */
public interface ReflectionInjectionRemediator {

  /** Remediate all reflection injection vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);

  ReflectionInjectionRemediator DEFAULT = new DefaultReflectionInjectionRemediator();
}
