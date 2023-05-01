package io.codemodder;

import com.github.javaparser.JavaParser;
import io.codemodder.codetf.CodeTFResult;
import java.nio.file.Path;
import java.util.List;

public interface CodemodExecutor {

  CodeTFResult execute(List<Path> filePaths);

  static CodemodExecutor from(
      final Path projectDir,
      final IncludesExcludes includesExcludes,
      final CodemodIdPair codemod,
      final List<ProjectProvider> projectProviders,
      final JavaParser javaParser,
      final EncodingDetector encodingDetector) {
    return new DefaultCodemodExecutor(
        projectDir, includesExcludes, codemod, projectProviders, javaParser, encodingDetector);
  }
}
