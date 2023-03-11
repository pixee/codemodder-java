package io.codemodder.codemods;

import static io.codemodder.CodemodInvoker.run;

/** Invokes the codemod from a command line. */
public final class Runner {

  public static void main(final String[] args) {
    run(SecureRandomCodemod.class);
  }
}
