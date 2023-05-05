package io.codemodder;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Parses a list of sarif {@link File}s to a {@link Map} of {@link RuleSarif}s organized by tool
 * name.
 */
public interface SarifParser {

  /**
   * Given a list of sarif {@link File}s, organize them into a {@Link Map} containing {@link
   * RuleSarif}s organized by tool name.
   */
  Map<String, List<RuleSarif>> parseIntoMap(List<File> sarifFiles, Path repositoryRoot);

  static SarifParser create() {
    return new DefaultSarifParser();
  }
}
