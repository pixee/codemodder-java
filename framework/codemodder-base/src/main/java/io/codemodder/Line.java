package io.codemodder;

/** Represents a line of code. */
public interface Line {
  /**
   * @return the line number, starting from 1
   */
  int number();

  /**
   * @return the content of the line
   */
  String content();
}
