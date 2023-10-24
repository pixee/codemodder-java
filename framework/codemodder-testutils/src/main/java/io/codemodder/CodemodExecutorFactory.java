package io.codemodder;

import io.codemodder.javaparser.JavaParserFacade;
import java.nio.file.Path;
import java.util.List;

/** A utility for generating a {@link CodemodExecutor} for testing, with some sane defaults. */
public interface CodemodExecutorFactory {

  static CodemodExecutor from(
      final Path projectDir,
      final IncludesExcludes includesExcludes,
      final CodemodIdPair codemod,
      final List<ProjectProvider> projectProviders,
      final List<CodeTFProvider> codetfProviders,
      final FileCache fileCache,
      final JavaParserFacade javaParser,
      final EncodingDetector encodingDetector) {
    return new DefaultCodemodExecutor(
        projectDir,
        includesExcludes,
        codemod,
        projectProviders,
        codetfProviders,
        fileCache,
        javaParser,
        encodingDetector,
        10_000_000,
        10_000,
        -1);
  }
}
