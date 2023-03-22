package io.codemodder;

import java.io.IOException;

/** Gives access to raw files for performing arbitrary cahnges. */
public interface RawFileChanger extends Changer {

  /**
   * Visit a file. It is up to the subtype to make sure the file is something to be changed and
   * perform all the changing.
   */
  void visitFile(CodemodInvocationContext context) throws IOException;
}
