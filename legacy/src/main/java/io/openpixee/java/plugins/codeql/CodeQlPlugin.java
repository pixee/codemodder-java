package io.openpixee.java.plugins.codeql;

import com.contrastsecurity.sarif.*;
import com.google.common.annotations.VisibleForTesting;
import io.codemodder.CodemodRegulator;
import io.openpixee.java.DefaultSarifProcessorPlugin;
import io.openpixee.java.FileBasedVisitor;
import io.openpixee.java.VisitorFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * This type is responsible for implementing the protection of issues detected by the CodeQL scanner
 * that aren't redundant to protections built in to pixee.
 */
public final class CodeQlPlugin extends DefaultSarifProcessorPlugin {

  @Override
  public boolean supports(final Tool tool) {
    final String name = tool.getDriver().getName();
    return "CodeQL".equals(name);
  }

  @Override
  protected List<VisitorFactory> getVendorToolSpecificFactories(
      final File repositoryRoot, final Run run, final CodemodRegulator codemodRegulator) {
    final Map<String, Set<Result>> ruleIdToResultsMap = getRuleIdToResultsMap(run);
    final List<VisitorFactory> visitors = new ArrayList<>();
    final Set<Map.Entry<String, Set<Result>>> ruleFindings = ruleIdToResultsMap.entrySet();
    for (final Map.Entry<String, Set<Result>> ruleFinding : ruleFindings) {
      final String ruleId = ruleFinding.getKey();
      if (codemodRegulator.isAllowed(ruleId)) {
        if ("java/missing-jwt-signature-check".equals(ruleId)) {
          visitors.add(
              new UnverifiedJwtParseVisitorFactory(repositoryRoot, ruleFinding.getValue()));
        } else if ("java/insecure-cookie".equals(ruleId)) {
          visitors.add(new InsecureCookieVisitorFactory(repositoryRoot, ruleFinding.getValue()));
        } else if ("java/database-resource-leak".equals(ruleId)) {
          visitors.add(new JDBCResourceLeakVisitorFactory(repositoryRoot, ruleFinding.getValue()));
        }
      }
    }
    return Collections.unmodifiableList(visitors);
  }

  /**
   * In the SARIF document, each result/finding contains a reference index to the rule it's
   * associated with. This function creates a {@link Map} that links each rule found during the scan
   * to its corresponding findings/results.
   */
  @VisibleForTesting
  @NotNull
  Map<String, Set<Result>> getRuleIdToResultsMap(final Run run) {

    final Optional<ReportingDescriptor[]> maybeRules =
        run.getTool().getExtensions().stream()
            .filter(ext -> "codeql/java-queries".equals(ext.getName()))
            .findFirst()
            .map(ToolComponent::getRules)
            .map(s -> s.toArray(new ReportingDescriptor[0]));

    if (maybeRules.isPresent()) {
      final var rules = maybeRules.get();
      // map the findings to their given rule
      final Map<String, Set<Result>> ruleIdToResultsMap = new HashMap<>();
      run.getResults()
          .forEach(
              (entry) -> {
                final ReportingDescriptorReference ruleReference = entry.getRule();
                final Integer ruleIndex = ruleReference.getIndex();
                final ReportingDescriptor rule = rules[ruleIndex];
                final Set<Result> results =
                    ruleIdToResultsMap.computeIfAbsent(rule.getName(), (k) -> new HashSet<>());
                results.add(entry);
              });
      return Collections.unmodifiableMap(ruleIdToResultsMap);
    } else return Collections.emptyMap();
  }

  @Override
  public List<FileBasedVisitor> getFileWeaversFor(
      final File repositoryRoot, final Run run, final CodemodRegulator context) {
    final Set<Result> pomResults = getPOMResults(run, "java/maven/non-https-url");
    final var weavers = new ArrayList<FileBasedVisitor>();
    if (!pomResults.isEmpty()) {
      weavers.add(new MavenSecureURLVisitor(repositoryRoot, pomResults));
    }
    return weavers;
  }

  @NotNull
  private Set<Result> getPOMResults(final Run run, final String ruleId) {
    return run.getResults().stream()
        .filter(r -> ruleId.equals(r.getRuleId()))
        .filter(
            r ->
                r.getLocations()
                    .get(0)
                    .getPhysicalLocation()
                    .getArtifactLocation()
                    .getUri()
                    .toLowerCase()
                    .endsWith("xml"))
        .collect(Collectors.toUnmodifiableSet());
  }
}
