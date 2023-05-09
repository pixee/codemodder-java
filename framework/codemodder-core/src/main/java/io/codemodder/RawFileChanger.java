package io.codemodder;

import java.io.IOException;
import java.util.List;

/** Gives access to raw files for performing arbitrary changes. */
public interface RawFileChanger extends CodeChanger {

  /**
   * Visit a file. It is up to the subtype to make sure the file is something to be changed and
   * perform all the changing.
   *
   * @return a list of changes that were made to the file
   */
  List<CodemodChange> visitFile(CodemodInvocationContext context) throws IOException;
}
