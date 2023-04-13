package io.codemodder;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses a list of sarif {@link File}s to a {@link Map} of {@link RuleSarif}s organized by tool
 * name.
 */
public interface SarifParser {

  /** Given a list of sarif {@link File}s, organize them into a {@Link Map} containing {@link RuleSarif}s organized by tool name. */
  Map<String, List<RuleSarif>> parseIntoMap(List<File> sarifFiles, Path repositoryRoot);

  final class Default implements SarifParser {

    private Optional<SarifSchema210> readSarifFile(final File sarifFile) {
      try {
        return Optional.of(
            new ObjectMapper().readValue(new FileReader(sarifFile), SarifSchema210.class));
      } catch (final IOException e) {
        LOG.error("Problem deserializing SARIF file: {}", sarifFile, e);
        return Optional.empty();
      }
    }

    /** Send the arguments to all factories and returns the first that built something. */
    private Optional<Map.Entry<String, RuleSarif>> tryToBuild(
        final String toolName,
        final String rule,
        final SarifSchema210 sarif,
        final Path repositoryRoot,
        final List<RuleSarifFactory> factories) {
      for (final var factory : factories) {
        final var maybeRuleSarif = factory.build(toolName, rule, sarif, repositoryRoot);
        if (maybeRuleSarif.isPresent()) {
          return Optional.of(Map.entry(toolName, maybeRuleSarif.get()));
        }
      }

      LOG.info("Found SARIF from unsupported tool: {}", toolName);
      return Optional.empty();
    }

    private Stream<Map.Entry<String, RuleSarif>> fromSarif(
        final Run run, final SarifSchema210 sarif, final Path repositoryRoot) {
      // driver name
      final var toolName = run.getTool().getDriver().getName();
      final List<RuleSarifFactory> factories =
          ServiceLoader.load(RuleSarifFactory.class).stream()
              .map(ServiceLoader.Provider::get)
              .collect(Collectors.toUnmodifiableList());
      final var allResults =
          run.getResults().stream().map(Result::getRuleId).filter(Objects::nonNull).distinct();

      return allResults.flatMap(
          rule -> tryToBuild(toolName, rule, sarif, repositoryRoot, factories).stream());
    }

    /**
     * Parse a list of SARIF files and organize the obtained {@link RuleSarif}s by tool name with a
     * map .
     */
    public Map<String, List<RuleSarif>> parseIntoMap(
        final List<File> sarifFiles, final Path repositoryRoot) {
      final var map = new HashMap<String, List<RuleSarif>>();
      sarifFiles.stream()
          .flatMap(f -> readSarifFile(f).stream())
          .flatMap(
              sarif ->
                  sarif.getRuns().stream().flatMap(run -> fromSarif(run, sarif, repositoryRoot)))
          .forEach(
              p ->
                  map.merge(
                      p.getKey(),
                      new ArrayList<>(Collections.singletonList(p.getValue())),
                      (l1, l2) -> {
                        l1.add(l2.get(0));
                        return l1;
                      }));
      return map;
    }

    private static final Logger LOG = LoggerFactory.getLogger(Default.class);
  }
}
