package io.codemodder;

import io.codemodder.codetf.CodeTFResult;
import java.nio.file.Path;
import java.util.List;

/** A type responsible for executing a codemod on a set of files. */
public interface CodemodExecutor {

  /** Execute the codemod on the given file paths. */
  CodeTFResult execute(List<Path> filePaths);
}
