package io.codemodder;

import java.nio.file.Path;
import java.util.Objects;

record DefaultCodemodInvocationContext(
    CodeDirectory codeDirectory,
    Path path,
    String codemodId,
    LineIncludesExcludes lineIncludesExcludes)
    implements CodemodInvocationContext {

  DefaultCodemodInvocationContext {
    Objects.requireNonNull(codeDirectory);
    Objects.requireNonNull(path);
    Objects.requireNonNull(codemodId);
    Objects.requireNonNull(lineIncludesExcludes);
  }
}
