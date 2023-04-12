package io.codemodder.testutils;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.utils.Pair;
import io.codemodder.RuleSarif;
import io.codemodder.providers.sarif.codeql.CodeQLRuleSarif;
import io.codemodder.providers.sarif.semgrep.SemgrepRuleSarif;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a list of sarif {@link File}s to a {@link Map} of {@link RuleSarif}s organized by tool
 * name.
 */
public class StaticSarifParser {

  private static Optional<SarifSchema210> readSarifFile(final File sarifFile) {
    try {
      return Optional.of(
          new ObjectMapper().readValue(new FileReader(sarifFile), SarifSchema210.class));
    } catch (final IOException e) {
      LOG.error("Problem deserializing SARIF file: {}", sarifFile, e);
      return Optional.empty();
    }
  }

  private static Stream<Pair<String, RuleSarif>> fromSarif(
      final Run run, final SarifSchema210 sarif, final Path repositoryRoot) {
    // driver name
    final var toolName = run.getTool().getDriver().getName();
    final var allResults =
        run.getResults().stream().map(Result::getRuleId).filter(Objects::nonNull).distinct();
    if (toolName.equals("semgrep")) {
      return allResults.map(rule -> new Pair<>(toolName, new SemgrepRuleSarif(rule, sarif)));
    }
    if (toolName.equals("CodeQL")) {
      return allResults.map(
          rule -> new Pair<>(toolName, new CodeQLRuleSarif(rule, sarif, repositoryRoot)));
    }
    LOG.info("Found SARIF from unsupported tool: {}", toolName);
    return Stream.empty();
  }

  /**
   * Parse a list of SARIF files and organize the obtained {@link RuleSarif}s by tool name with a
   * map .
   */
  public static Map<String, List<RuleSarif>> parseIntoMap(
      final List<File> sarifFiles, final Path repositoryRoot) {
    final var map = new HashMap<String, List<RuleSarif>>();
    sarifFiles.stream()
        .flatMap(f -> readSarifFile(f).stream())
        .flatMap(
            sarif -> sarif.getRuns().stream().flatMap(run -> fromSarif(run, sarif, repositoryRoot)))
        .forEach(
            p ->
                map.merge(
                    p.a,
                    new ArrayList<>(Collections.singletonList(p.b)),
                    (l1, l2) -> {
                      l1.add(l2.get(0));
                      return l1;
                    }));
    return map;
  }

  private static final Logger LOG = LoggerFactory.getLogger(StaticSarifParser.class);
}
