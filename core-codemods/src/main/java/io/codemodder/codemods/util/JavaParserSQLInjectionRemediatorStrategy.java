package io.codemodder.codemods.util;

import com.github.javaparser.ast.CompilationUnit;
import io.codemodder.CodemodFileScanningResult;
import io.codemodder.CodemodInvocationContext;
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
   * @param context the context of the codemod invocation
   * @param cu the compilation unit to be scanned
   * @param pathFindings a collection of findings to be processed
   * @param detectorRule the rule used to detect potential issues
   * @param findingIdExtractor a function to extract the ID from a finding
   * @param findingLineExtractor a function to extract the line number from a finding
   * @param <T> the type of the findings
   * @return a result object containing the changes and unfixed findings
   */
  <T> CodemodFileScanningResult visit(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final Collection<T> pathFindings,
      final DetectorRule detectorRule,
      final Function<T, String> findingIdExtractor,
      final Function<T, Integer> findingLineExtractor);

  JavaParserSQLInjectionRemediatorStrategy DEFAULT =
      new DefaultJavaParserSQLInjectionRemediatorStrategy();
}
