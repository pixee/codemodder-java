package io.codemodder.remediation;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;

public interface RemediationStrategy {

  /**
   * Apply a fix to the issue detected a given node.
   *
   * @return A SuccessOrReason object containing a list of dependencies if the fix was successful,
   *     or a reason for failure otherwise
   */
  SuccessOrReason fix(CompilationUnit cu, Node node);
}
