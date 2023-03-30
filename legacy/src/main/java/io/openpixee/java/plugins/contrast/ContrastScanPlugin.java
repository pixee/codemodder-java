package io.openpixee.java.plugins.contrast;

import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.Location;
import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.Run;
import com.contrastsecurity.sarif.Tool;
import com.contrastsecurity.sarif.ToolComponent;
import com.google.common.annotations.VisibleForTesting;
import io.codemodder.RuleContext;
import io.openpixee.java.DefaultSarifProcessorPlugin;
import io.openpixee.java.FileBasedVisitor;
import io.openpixee.java.VisitorFactory;
import io.openpixee.java.plugins.ReflectionInjectionVisitorFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This type is responsible for implementing the protection of issues detected by the Contrast Scan
 * static analysis tool that aren't redundant to protections built in to pixee. Their version of
 * SARIF is meaningfully different (more simple) than CodeQL.
 */
public final class ContrastScanPlugin extends DefaultSarifProcessorPlugin {

  @Override
  public boolean supports(final Tool tool) {
    ToolComponent driver = tool.getDriver();
    final String name = driver.getName();
    return "Contrast Scan".equals(name);
  }

  @Override
  protected List<VisitorFactory> getVendorToolSpecificFactories(
      final File repositoryRoot, final Run run, final RuleContext ruleContext) {

    List<Result> results = run.getResults();
    List<VisitorFactory> visitorFactories = new ArrayList<>();

    if (ruleContext.isRuleAllowed("contrast:java/reflected-xss")) {
      Set<Map.Entry<String, Set<Result>>> xssEntries =
          getRuleEntries(results, List.of("reflected-xss"));
      for (final Map.Entry<String, Set<Result>> xssEntry : xssEntries) {
        visitorFactories.add(
            new JavaXssVisitorFactory(repositoryRoot, xssEntry.getValue(), "reflected-xss"));
      }
    }

    if (ruleContext.isRuleAllowed("contrast:java/stored-xss")) {
      Set<Map.Entry<String, Set<Result>>> xssEntries =
          getRuleEntries(results, List.of("stored-xss"));
      for (final Map.Entry<String, Set<Result>> xssEntry : xssEntries) {
        visitorFactories.add(
            new JavaXssVisitorFactory(repositoryRoot, xssEntry.getValue(), "stored-xss"));
      }
    }

    if (ruleContext.isRuleAllowed(ReflectionInjectionVisitorFactory.ID)) {
      Set<Map.Entry<String, Set<Result>>> reflectionInjectionEntries =
          getRuleEntries(results, List.of("reflection-injection"));
      for (final Map.Entry<String, Set<Result>> reflectionInjectionEntry :
          reflectionInjectionEntries) {
        visitorFactories.add(
            new ReflectionInjectionVisitorFactory(
                repositoryRoot, reflectionInjectionEntry.getValue()));
      }
    }
    return Collections.unmodifiableList(visitorFactories);
  }

  @NotNull
  @VisibleForTesting
  Set<Map.Entry<String, Set<Result>>> getRuleEntries(
      final List<Result> results, List<String> ruleIds) {
    Map<String, Set<Result>> rulePerFileResults = new HashMap<>();
    for (Result result : results) {
      if (isJavaResult(result)) {
        String ruleId = result.getRuleId();
        if (ruleIds.contains(ruleId)) {
          String location = getLocation(result);
          Set<Result> xssResults =
              rulePerFileResults.computeIfAbsent(location, (k) -> new HashSet<>());
          xssResults.add(result);
        }
      }
    }
    return rulePerFileResults.entrySet();
  }

  private boolean isJavaResult(final Result result) {
    String uri = getLocation(result);
    if (uri == null) return false;
    return uri.endsWith(".java");
  }

  @Nullable
  private String getLocation(final Result result) {
    List<Location> locations = result.getLocations();
    if (locations.isEmpty()) {
      LOG.debug("Ignoring Contrast result with no location: {}", result.getCorrelationGuid());
      return null;
    } else if (locations.size() > 1) {
      LOG.debug(
          "Ignoring Contrast result with multiple locations: {}", result.getCorrelationGuid());
      return null;
    }
    Location location = locations.get(0);
    PhysicalLocation physicalLocation = location.getPhysicalLocation();
    if (physicalLocation == null) {
      LOG.debug("Ignoring Contrast result no physical location: {}", result.getCorrelationGuid());
      return null;
    }
    ArtifactLocation artifactLocation = physicalLocation.getArtifactLocation();
    if (artifactLocation == null) {
      LOG.debug("Ignoring Contrast result no artifact location: {}", result.getCorrelationGuid());
      return null;
    }
    return artifactLocation.getUri();
  }

  @Override
  public List<FileBasedVisitor> getFileWeaversFor(
      final File repositoryRoot, final Run run, RuleContext context) {
    List<Result> storedJspXss = getXssJspResults(run, "stored-xss");
    List<Result> reflectedJspXss = getXssJspResults(run, "reflected-xss");
    List<FileBasedVisitor> weavers = new ArrayList<>();
    if (!reflectedJspXss.isEmpty()) {
      weavers.add(new SarifBasedJspScriptletXSSVisitor(reflectedJspXss, "reflected-xss"));
    }
    if (!storedJspXss.isEmpty()) {
      weavers.add(new SarifBasedJspScriptletXSSVisitor(storedJspXss, "stored-xss"));
    }
    return weavers;
  }

  @NotNull
  private List<Result> getXssJspResults(final Run run, final String ruleId) {
    return run.getResults().stream()
        .filter(r -> ruleId.equals(r.getRuleId()))
        .filter(
            r ->
                r.getLocations()
                    .get(0)
                    .getPhysicalLocation()
                    .getArtifactLocation()
                    .getUri()
                    .endsWith(".jsp"))
        .collect(Collectors.toUnmodifiableList());
  }

  static final String ruleBase = "contrast-scan:java/";
  private static final Logger LOG = LoggerFactory.getLogger(ContrastScanPlugin.class);
}
