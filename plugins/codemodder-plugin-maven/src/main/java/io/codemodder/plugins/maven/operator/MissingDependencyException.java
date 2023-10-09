package io.codemodder.plugins.maven.operator;

public class MissingDependencyException extends RuntimeException {
  public MissingDependencyException(String message) {
    super(message);
  }
}
