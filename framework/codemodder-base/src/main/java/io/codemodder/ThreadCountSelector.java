package io.codemodder;

/** Provides a way to select the best number of threads to use for heavy file I/O. */
@FunctionalInterface
interface ThreadCountSelector {

  /** Return the number of threads to use for heavy file I/O. */
  int count();
}
