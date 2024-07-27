package io.codemodder.remediation.xxe;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.List;
import java.util.function.Function;

/**
 * Strategy for remediating XXE vulnerabilities at an intermediate step in {@link
 * javax.xml.stream.XMLInputFactory#createXMLStreamReader}.
 */
public interface XXEIntermediateXMLStreamReaderRemediator {

  /** A default implementation for callers. */
  XXEIntermediateXMLStreamReaderRemediator DEFAULT =
      new DefaultXXEIntermediateXMLStreamReaderRemediator();

  /** Remediate all XXE vulnerabilities in the given compilation unit. */
  <T> CodemodFileScanningResult remediateAll(
      CompilationUnit cu,
      String path,
      DetectorRule detectorRule,
      List<T> issuesForFile,
      Function<T, String> getKey,
      Function<T, Integer> getLine,
      Function<T, Integer> getColumn);
}
