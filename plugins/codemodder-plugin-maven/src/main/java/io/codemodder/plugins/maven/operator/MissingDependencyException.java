package io.codemodder.plugins.maven.operator;

class MissingDependencyException extends RuntimeException {
  public MissingDependencyException(String message) {
    super(message);
  }
}
