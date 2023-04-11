package io.openpixee.java;

import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.SarifSchema210;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.codemodder.RuleSarif;
import io.codemodder.RuleSarifProvider;
import io.codemodder.providers.sarif.codeql.CodeQLRuleSarif;
import io.codemodder.providers.sarif.semgrep.SemgrepRuleSarif;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A provider of {@link RuleSarif}s that uses a static map. */
public class StaticRuleSarifProvider implements RuleSarifProvider {

  private static ThreadLocal<HashMap<String, HashSet<RuleSarif>>> sarifs =
      ThreadLocal.withInitial(HashMap::new);
  private static Path repositoryRoot;

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
      final Run run, final SarifSchema210 sarif) {
    // driver name
    var toolName = run.getTool().getDriver().getName();
    var allResults =
        run.getResults().stream()
            .map(result -> result.getRuleId())
            .filter(Objects::nonNull)
            .distinct();
    if (toolName.equals(SemgrepRuleSarif.TOOL_NAME)) {
      return allResults.map(rule -> new Pair<>(toolName, new SemgrepRuleSarif(rule, sarif)));
    }
    if (toolName.equals(CodeQLRuleSarif.TOOL_NAME)) {
      return allResults.map(
          rule -> new Pair<>(toolName, new CodeQLRuleSarif(rule, sarif, repositoryRoot)));
    }
    LOG.info("Found SARIF from unsupported tool: {}", toolName);
    return Stream.empty();
  }

  /** Parse a list of SARIF files and builds the map. */
  public static void parseAndSet(List<File> sarifFiles, Path repositoryRoot) {
    StaticRuleSarifProvider.repositoryRoot = repositoryRoot;
    var map = new HashMap<String, HashSet<RuleSarif>>();
    sarifFiles.stream()
        .flatMap(f -> readSarifFile(f).stream())
        .flatMap(sarif -> sarif.getRuns().stream().flatMap(run -> fromSarif(run, sarif)))
        .forEach(
            p ->
                map.merge(
                    p.getValue0(),
                    new HashSet<>(List.of(p.getValue1())),
                    (hs1, hs2) -> {
                      hs1.add(p.getValue1());
                      return hs1;
                    }));
    ;
    sarifs.set(map);
  }

  @Override
  public Set<RuleSarif> getAllRuleSarifs() {
    return sarifs.get().entrySet().stream()
        .flatMap(e -> e.getValue().stream())
        .collect(Collectors.toSet());
  }

  @Override
  public Set<RuleSarif> getRuleSarifsByRuleId(List<String> ruleIdList) {
    return sarifs.get().entrySet().stream()
        .flatMap(e -> e.getValue().stream())
        .filter(rs -> ruleIdList.stream().anyMatch(id -> id.equals(rs.getRule())))
        .collect(Collectors.toSet());
  }

  @Override
  public Set<RuleSarif> getRuleSarifsByTool(String tool) {
    return sarifs.get().getOrDefault(tool, new HashSet<>());
  }

  private static final Logger LOG = LoggerFactory.getLogger(StaticRuleSarifProvider.class);
}
