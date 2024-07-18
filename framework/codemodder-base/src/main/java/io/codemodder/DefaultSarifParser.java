package io.codemodder;

import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DefaultSarifParser implements SarifParser {

  private Optional<SarifSchema210> readSarifFile(final Path sarifFile) {
    try {
      return Optional.of(
          new ObjectMapper().readValue(Files.newInputStream(sarifFile), SarifSchema210.class));
    } catch (final IOException e) {
      LOG.error("Problem deserializing SARIF file: {}", sarifFile, e);
      return Optional.empty();
    }
  }

  /** Send the arguments to all factories and returns the first that built something. */
  private Optional<Map.Entry<String, RuleSarif>> tryToBuild(
      final String toolName,
      final RuleDescriptor rule,
      final SarifSchema210 sarif,
      final CodeDirectory codeDirectory,
      final List<RuleSarifFactory> factories) {
    for (final var factory : factories) {
      final var maybeRuleSarif =
          factory.build(toolName, rule.ruleId, rule.messageText, sarif, codeDirectory);
      if (maybeRuleSarif.isPresent()) {
        return Optional.of(Map.entry(toolName, maybeRuleSarif.get()));
      }
    }

    LOG.info("Found SARIF from unsupported tool: {}", toolName);
    return Optional.empty();
  }

  private record RuleDescriptor(String ruleId, String messageText) {}

  private RuleDescriptor extractRuleId(final Result result, final Run run) {
    if (result.getRuleId() == null) {
      var toolIndex = result.getRule().getToolComponent().getIndex();
      var ruleIndex = result.getRule().getIndex();
      var maybeRule =
          run.getTool().getExtensions().stream()
              .skip(toolIndex)
              .findFirst()
              .flatMap(tool -> tool.getRules().stream().skip(ruleIndex).findFirst())
              .map(descriptor -> new RuleDescriptor(descriptor.getId(), null));

      if (maybeRule.isPresent()) {
        return maybeRule.get();
      } else {
        LOG.info("Could not find rule id for result.");
        return null;
      }
    }
    return new RuleDescriptor(result.getRuleId(), result.getMessage().getText());
  }

  private Stream<Map.Entry<String, RuleSarif>> fromSarif(
      final Run run, final SarifSchema210 sarif, final CodeDirectory codeDirectory) {
    // driver name
    final var toolName = run.getTool().getDriver().getName();
    final List<RuleSarifFactory> factories =
        ServiceLoader.load(RuleSarifFactory.class).stream()
            .map(ServiceLoader.Provider::get)
            .toList();
    final var runResults = run.getResults();
    final var allResults =
        runResults != null
            ? runResults.stream()
                .map(result -> extractRuleId(result, run))
                .filter(Objects::nonNull)
                .distinct()
            : Stream.<RuleDescriptor>empty();

    return allResults.flatMap(
        rule -> tryToBuild(toolName, rule, sarif, codeDirectory, factories).stream());
  }

  /**
   * Parse a list of SARIF files and organize the obtained {@link RuleSarif}s by tool name with a
   * map .
   */
  @Override
  public Map<String, List<RuleSarif>> parseIntoMap(
      final List<Path> sarifFiles, final CodeDirectory codeDirectory) {
    final var map = new HashMap<String, List<RuleSarif>>();
    sarifFiles.stream()
        .flatMap(f -> readSarifFile(f).stream())
        .flatMap(
            sarif -> sarif.getRuns().stream().flatMap(run -> fromSarif(run, sarif, codeDirectory)))
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

  private static final Logger LOG = LoggerFactory.getLogger(DefaultSarifParser.class);
}
