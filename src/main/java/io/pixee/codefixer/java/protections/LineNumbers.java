package io.pixee.codefixer.java.protections;

public final class LineNumbers {

  private LineNumbers() {}

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
