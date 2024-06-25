package io.codemodder.remediation.jndiinjection;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import io.codemodder.DependencyGAV;
import java.util.List;

/** Strategy for fixing JNDI injection vulnerabilities. */
interface JNDIFixStrategy {

  /**
   * Fix the JNDI injection vulnerability.
   *
   * @param cu the compilation unit
   * @param parentClass the parent type (could be nested type)
   * @param lookupCall the lookup() method call
   * @param blockStmt the block which contains the lookup statement
   * @param index the index of the statement in the block
   * @param contextNameVariable the variable that holds the JNDI context
   * @return the list of dependencies that need to be added to the project
   */
  List<DependencyGAV> fix(
      CompilationUnit cu,
      ClassOrInterfaceDeclaration parentClass,
      MethodCallExpr lookupCall,
      NameExpr contextNameVariable,
      BlockStmt blockStmt,
      int index);
}
