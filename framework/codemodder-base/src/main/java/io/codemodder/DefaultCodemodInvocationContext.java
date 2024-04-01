package io.codemodder;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;

record DefaultCodemodInvocationContext(
    CodeDirectory codeDirectory,
    Path path,
    String contents,
    String codemodId,
    LineIncludesExcludes lineIncludesExcludes,
    Collection<DependencyGAV> dependencies)
    implements CodemodInvocationContext {

  DefaultCodemodInvocationContext {
    Objects.requireNonNull(codeDirectory);
    Objects.requireNonNull(path);
    Objects.requireNonNull(contents);
    Objects.requireNonNull(codemodId);
    Objects.requireNonNull(lineIncludesExcludes);
    Objects.requireNonNull(dependencies);
  }
}
