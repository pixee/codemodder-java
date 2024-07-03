package io.codemodder.remediation.xss;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

/** Remediates header injection vulnerabilities. */
public interface XSSRemediator {

  /** Remediate all header injection vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateJava(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);

  <T> CodemodFileScanningResult remediateJSP(
      Path filePath,
      String relativePath,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);

  /** The default header injection remediation strategy. */
  XSSRemediator DEFAULT = new DefaultXSSRemediator();
}
