package io.codemodder.plugins.maven.operator;

public class WrongDependencyTypeException extends RuntimeException {
  public WrongDependencyTypeException(String message) {
    super(message);
  }
}
