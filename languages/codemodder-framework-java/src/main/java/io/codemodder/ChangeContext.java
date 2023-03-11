package io.codemodder;

/** Holds information about the */
public interface ChangeContext {

  void record(int line, int column);
}
