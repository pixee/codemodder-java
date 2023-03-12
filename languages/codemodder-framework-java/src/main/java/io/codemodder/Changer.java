package io.codemodder;

/** An empty interface that marks that a codemod type has provided some utility to change. */
public interface Changer {

  /** Get the ID. */
  String getCodemodId();
}
