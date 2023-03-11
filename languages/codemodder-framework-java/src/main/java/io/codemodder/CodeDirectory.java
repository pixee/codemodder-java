package io.codemodder;

import java.io.File;
import java.nio.file.Path;

/** Holds a code directory (e.g., a repository root). */
public interface CodeDirectory {

  File asFile();

  Path asPath();
}
