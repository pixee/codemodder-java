package io.codemodder;

import io.codemodder.codetf.CodeTFResult;
import io.codemodder.javaparser.JavaCache;
import java.nio.file.Path;
import java.util.List;

/** A type responsible for executing a codemod on a set of files. */
public interface CodemodExecutor {

  /** Execute the codemod on the given file paths. */
  CodeTFResult execute(List<Path> filePaths);

  static CodemodExecutor from(
      final Path projectDir,
      final IncludesExcludes includesExcludes,
      final CodemodIdPair codemod,
      final List<ProjectProvider> projectProviders,
      final List<CodeTFProvider> codetfProviders,
      final FileCache fileCache,
      final JavaCache javaParser,
      final EncodingDetector encodingDetector) {
    return new DefaultCodemodExecutor(
        projectDir,
        includesExcludes,
        codemod,
        projectProviders,
        codetfProviders,
        fileCache,
        javaParser,
        encodingDetector);
  }
}
