package io.pixee.codefixer.java.plugins.codeql;

import com.contrastsecurity.sarif.ReportingDescriptor;
import com.contrastsecurity.sarif.ReportingDescriptorReference;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.Tool;
import com.google.common.annotations.VisibleForTesting;
import io.pixee.codefixer.java.DefaultSarifProcessorPlugin;
import io.pixee.codefixer.java.FileBasedVisitor;
import io.pixee.codefixer.java.RuleContext;
import io.pixee.codefixer.java.VisitorFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
      final File repositoryRoot, final Run run, final RuleContext ruleContext) {
    Map<String, Set<Result>> ruleIdToResultsMap = getRuleIdToResultsMap(run);
    List<VisitorFactory> visitors = new ArrayList<>();
    Set<Map.Entry<String, Set<Result>>> ruleFindings = ruleIdToResultsMap.entrySet();
    for (final Map.Entry<String, Set<Result>> ruleFinding : ruleFindings) {
      String ruleId = ruleFinding.getKey();
      if (ruleContext.isRuleAllowed(ruleId)) {
        if ("java/stack-trace-exposure".equals(ruleId)) {
          visitors.add(
              new StackTraceExposureVisitorFactory(repositoryRoot, ruleFinding.getValue()));
        } else if ("java/missing-jwt-signature-check".equals(ruleId)) {
          visitors.add(
              new UnverifiedJwtParseVisitorFactory(repositoryRoot, ruleFinding.getValue()));
        } else if ("java/insecure-cookie".equals(ruleId)) {
          visitors.add(new InsecureCookieVisitorFactory(repositoryRoot, ruleFinding.getValue()));
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
    ReportingDescriptor[] rules =
        run.getTool().getExtensions().stream()
            .filter(ext -> "codeql/java-queries".equals(ext.getName()))
            .findFirst()
            .orElseThrow()
            .getRules()
            .toArray(new ReportingDescriptor[0]);

    // map the findings to their given rule
    Map<String, Set<Result>> ruleIdToResultsMap = new HashMap<>();
    run.getResults()
        .forEach(
            (entry) -> {
              ReportingDescriptorReference ruleReference = entry.getRule();
              Integer ruleIndex = ruleReference.getIndex();
              ReportingDescriptor rule = rules[ruleIndex];
              Set<Result> results =
                  ruleIdToResultsMap.computeIfAbsent(rule.getName(), (k) -> new HashSet<>());
              results.add(entry);
            });
    return Collections.unmodifiableMap(ruleIdToResultsMap);
  }

  @Override
  public List<FileBasedVisitor> getFileWeaversFor(final File repositoryRoot, final Run run) {
    return Collections.emptyList();
  }
}
