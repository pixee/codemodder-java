package io.codemodder;

import java.nio.file.Path;

/** Holds a code directory (e.g., a repository root). */
public interface CodeDirectory {

  /** The filesystem directory path we are running against. */
  Path asPath();
}
