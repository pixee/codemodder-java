package io.codemodder.plugins.maven.operator;

/**
 * An exception class representing the scenario where a required dependency is missing. This
 * exception is typically thrown when an operation or process relies on a certain dependency that is
 * not available or cannot be resolved.
 */
class MissingDependencyException extends RuntimeException {
  /**
   * Constructs a new `MissingDependencyException` with the specified error message.
   *
   * @param message A descriptive error message that provides information about the missing
   *     dependency.
   */
  public MissingDependencyException(String message) {
    super(message);
  }
}
