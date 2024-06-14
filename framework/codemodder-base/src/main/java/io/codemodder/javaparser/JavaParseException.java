package io.codemodder.javaparser;

import java.io.IOException;

/** Exception that indicates a failure to parse a Java file. */
public class JavaParseException extends IOException {
  public JavaParseException(final String message) {
    super(message);
  }
}
