package io.codemodder;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Parses a list of sarif {@link Path}s to a {@link Map} of {@link RuleSarif}s organized by tool
 * name.
 */
public interface SarifParser {

  /**
   * Given a list of sarif {@link Path}s, organize them into a {@link Map} containing {@link
   * RuleSarif}s organized by tool name.
   */
  Map<String, List<RuleSarif>> parseIntoMap(List<Path> sarifFiles, CodeDirectory codeDirectory);

  static SarifParser create() {
    return new DefaultSarifParser();
  }
}
