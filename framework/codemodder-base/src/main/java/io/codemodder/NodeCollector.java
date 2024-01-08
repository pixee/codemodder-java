package io.codemodder;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import java.util.List;

/** A type that can collect specific types of nodes from a {@link CompilationUnit}. */
public interface NodeCollector {

  /** Collects nodes of the specified type from the {@link CompilationUnit}. */
  List<? extends Node> collectNodes(final CompilationUnit cu, final Class<? extends Node> nodeType);

  /** A {@link NodeCollector} implementation that collects all nodes of a specified type. */
  NodeCollector ALL_FROM_TYPE = Node::findAll;

  /**
   * A {@link NodeCollector} implementation that collects all comments from a {@link
   * CompilationUnit}.
   */
  NodeCollector ALL_COMMENTS = (cu, nodeType) -> cu.getAllComments();
}
