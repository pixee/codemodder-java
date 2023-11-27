package io.codemodder.plugins.maven.operator;

/**
 * An exception that indicates the packaging type is incorrect for parent POMs or the main POM file.
 */
class WrongDependencyTypeException extends RuntimeException {

  /**
   * Constructs a new WrongDependencyTypeException with the specified error message.
   *
   * @param message The error message describing the nature of the exception.
   */
  public WrongDependencyTypeException(String message) {
    super(message);
  }
}
