package io.codemodder.plugins.maven.operator;

/** An exception that indicates a wrong dependency type was encountered. */
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
