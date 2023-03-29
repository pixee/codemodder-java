package io.codemodder;

import java.nio.file.Path;

/** The context we provide to each codemod. */
public interface CodemodInvocationContext {

  /**
   * A "flight recorder" that you used to record the changes you make, which will eventually be used
   * to build a report.
   */
  FileWeavingContext changeRecorder();

  /** The root directory where the project being transformed lives. */
  CodeDirectory codeDirectory();

  /** The individual file being changed. */
  Path path();

  /** The ID of the codemod changing the file. */
  String codemodId();
}
