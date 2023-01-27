package io.openpixee.codetl.test.integration.junit;

public class CodeTLExecutionException extends RuntimeException {

  public CodeTLExecutionException(final int code, final String err, final Throwable cause) {
    super("Exited with code " + code + "\n" + err, cause);
  }

  public CodeTLExecutionException(final int code, final String err) {
    this(code, err, null);
  }
}
