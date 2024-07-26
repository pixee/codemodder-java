package io.codemodder.remediation.javadeserialization;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/** Remediates Java deserialization vulnerabilities. */
public interface JavaDeserializationRemediator {

  /** Remediate all Java deserialization vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);

  /** The default header injection remediation strategy. */
  JavaDeserializationRemediator DEFAULT = new DefaultJavaDeserializationRemediator();
}
