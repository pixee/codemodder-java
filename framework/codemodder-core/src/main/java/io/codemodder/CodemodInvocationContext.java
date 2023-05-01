package io.codemodder;

import java.nio.file.Path;

/** The context we provide to each codemod. */
public interface CodemodInvocationContext {

  /** The includes/excludes for the file being changed. */
  LineIncludesExcludes lineIncludesExcludes();

  /** The root directory where the project being transformed lives. */
  CodeDirectory codeDirectory();

  /** The individual file being changed. */
  Path path();

  /** The ID of the codemod changing the file. */
  String codemodId();
}
