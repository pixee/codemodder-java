package io.codemodder.plugins.maven.operator;

class WrongDependencyTypeException extends RuntimeException {
  public WrongDependencyTypeException(String message) {
    super(message);
  }
}
