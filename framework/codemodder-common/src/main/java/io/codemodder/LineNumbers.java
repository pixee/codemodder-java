package io.codemodder;

/**
 * Utilities related to line numbers.
 */
public final class LineNumbers {

  private LineNumbers() {}

  /**
   * Get the line number you'd expect from an IDE at the given character offset.
   */
  public static int getLineNumberAt(final CharSequence str, final int index) {
    int linesSeen = 1;
    for (int i = 0; i < index; i++) {
      if (str.charAt(i) == '\n') {
        linesSeen++;
      }
    }
    return linesSeen;
  }
}
