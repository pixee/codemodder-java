package io.openpixee.codetl.test.integration.junit;

/**
 * Exception that describes a failed CodeTL executable invocation (i.e. the CodeTL process returned
 * an unsuccessful return code).
 */
public class CodeTLExecutionException extends RuntimeException {

  /**
   * @param code CodeTL program return code
   * @param err standard error from the CodeTL program
   */
  public CodeTLExecutionException(final int code, final String err) {
    super("Exited with code " + code + "\n" + err);
  }
}
