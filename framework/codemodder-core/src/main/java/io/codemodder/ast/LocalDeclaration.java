package io.codemodder.ast;

import com.github.javaparser.ast.Node;

/**
 * A local declaration. Either a local variable declaration, a formal parameter, or an exception
 * parameter.
 */
public interface LocalDeclaration {

  /** Returns the name used in this declaration as a String. */
  String getName();

  /** The node where the declaration occurs. */
  Node getDeclaration();

  /** The scope of this declaration. */
  LocalScope getScope();
}
