package io.codemodder;

import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.CachingJavaParser;
import java.nio.file.Path;
import java.util.List;

/** A type responsible for executing a codemod on a set of files. */
public interface CodemodExecutor {

  CodeTFResult execute(List<Path> filePaths);

  static CodemodExecutor from(
      final Path projectDir,
      final IncludesExcludes includesExcludes,
      final CodemodIdPair codemod,
      final List<ProjectProvider> projectProviders,
      final CachingJavaParser javaParser,
      final EncodingDetector encodingDetector) {
    return new DefaultCodemodExecutor(
        projectDir, includesExcludes, codemod, projectProviders, javaParser, encodingDetector);
  }
}