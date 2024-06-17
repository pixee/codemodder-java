package io.codemodder.codemods.util;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.codetf.DetectorRule;
import java.util.Collection;
import java.util.function.Function;

/**
 * Strategy interface for remediating SQL injection vulnerabilities using JavaParser.
 * Implementations of this interface define the method to visit a CompilationUnit and process
 * findings for potential SQL injections.
 */
public interface JavaParserSQLInjectionRemediatorStrategy {

  /**
   * Visits the provided CompilationUnit and processes findings for potential SQL injections.
   *
   * @param cu the compilation unit to be scanned
   * @param pathFindings a collection of findings to be processed
   * @param findingIdExtractor a function to extract the ID from a finding
   * @param findingLineExtractor a function to extract the line number from a finding
   * @param <T> the type of the findings
   * @return a result object containing the changes and unfixed findings
   */
  <T> CodemodFileScanningResult remediateAll(
      final CompilationUnit cu,
      final String path,
      final DetectorRule rule,
      final Collection<T> pathFindings,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingLineExtractor);

  /** A default implementation that should be used in all non-test scenarios. */
  JavaParserSQLInjectionRemediatorStrategy DEFAULT =
      new DefaultJavaParserSQLInjectionRemediatorStrategy();
}
