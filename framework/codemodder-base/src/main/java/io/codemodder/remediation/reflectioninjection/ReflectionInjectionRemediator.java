package io.codemodder.remediation.reflectioninjection;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Remediates reflection injection vulnerabilities. */
public interface ReflectionInjectionRemediator {

  /** Remediate all reflection injection vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      ToIntFunction<T> getLine,
      Function<T, OptionalInt> getColumn);

  default <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      ToIntFunction<T> getLine,
      ToIntFunction<T> getColumn) {
    return remediateAll(
        cu,
        path,
        detectorRule,
        issuesForFile,
        getKey,
        getLine,
        (Function<T, OptionalInt>) issue -> OptionalInt.of(getColumn.applyAsInt(issue)));
  }

  ReflectionInjectionRemediator DEFAULT = new DefaultReflectionInjectionRemediator();
}
