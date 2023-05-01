package io.openpixee.java.plugins.contrast;

import com.contrastsecurity.sarif.Result;
import io.codemodder.*;
import io.codemodder.CodemodChange;
import io.openpixee.java.FileBasedVisitor;
import io.openpixee.java.JspLineWeave;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The scope of this is to fix simple JSP expressions that our default protection wouldn't help
 * with. Some example:
 *
 * <p>In this example, the variable being output can't be identified by our protection as being
 * definitively user controlled:
 *
 * <pre>
 *     Hello &lt;%=name%&gt;
 * </pre>
 *
 * Or even:
 *
 * <pre>
 *     Hello ${name}
 * </pre>
 *
 * If we only have one result for that JSP line, it's easy to identify that it's that scriptlet (or
 * EL). To protect ourselves from some ambiguous behavior we follow these rules:
 *
 * <p>If there are more than 1 XSS from the SARIF results on a line, we skip those results If there
 * is exactly one JSP expression on the given vulnerable line... And there is no JSP action tag/JSP
 * EL on the given vulnerable line... We will attempt to secure the given JSP expression
 */
public final class SarifBasedJspScriptletXSSVisitor implements FileBasedVisitor {

  private final List<Result> xssResults;
  private String ruleId;

  public SarifBasedJspScriptletXSSVisitor(final List<Result> xssResults, final String ruleId) {
    this.xssResults = Objects.requireNonNull(xssResults);
    this.ruleId = Objects.requireNonNull(ruleId);
  }

  @Override
  public String ruleId() {
    return ContrastScanPlugin.ruleBase + ruleId;
  }

  /** A type that knows how to rewrite a line of JSP to fix an XSS vulnerability. */
  interface JspLineRewriterStrategy {
    Optional<JspLineWeave> rewriteJspLine(
        Set<Result> xssResults, List<String> allLines, String line);
  }

  private static class DefaultJspLineRewriterStrategy implements JspLineRewriterStrategy {

    private final Set<JspOutputMethod> jspOutputMethods;

    private DefaultJspLineRewriterStrategy(final Set<JspOutputMethod> jspOutputMethods) {
      this.jspOutputMethods = Objects.requireNonNull(jspOutputMethods);
    }

    @Override
    public Optional<JspLineWeave> rewriteJspLine(
        final Set<Result> xssResults, final List<String> allLines, final String line) {
      if (xssResults.size() > 1) {
        List<String> correlationIds =
            xssResults.stream().map(Result::getCorrelationGuid).collect(Collectors.toList());
        LOG.info(
            "Not patching {} vulns discovered for result: {}", xssResults.size(), correlationIds);
        return Optional.empty();
      } else if (xssResults.isEmpty()) {
        throw new IllegalArgumentException("expected XSS results");
      }

      Result result = xssResults.iterator().next();
      String correlationId = result.getCorrelationGuid();
      JspOutputMethod jspOutputMethod = null;
      for (JspOutputMethod candidateJspOutputMethod : jspOutputMethods) {
        int count = candidateJspOutputMethod.countPossibleWrites(line);
        if (count > 1) {
          LOG.info("Not patching {} vulns as {} JSP writes discovered", correlationId, count);
          return Optional.empty();
        } else if (count == 1) {
          if (jspOutputMethod == null) {
            LOG.info("Found JSP output for {}", correlationId);
            jspOutputMethod = candidateJspOutputMethod;
          } else {
            LOG.info(
                "Not patching {} as more than 1 type of JSP outputs discovered", correlationId);
            return Optional.empty();
          }
        }
      }
      if (jspOutputMethod != null) {
        try {
          JspLineWeave lineWeave =
              jspOutputMethod.weaveLine(line, ContrastScanPlugin.ruleBase + result.getRuleId());
          LOG.info("Rebuilt JSP line for {}", correlationId);
          return Optional.of(lineWeave);
        } catch (UnsupportedOperationException e) {
          LOG.error("Couldn't weave line as that JSP output method is not supported yet");
        } catch (IllegalArgumentException e) {
          LOG.error("Couldn't weave line as that JSP code couldn't be changed");
        }
      } else {
        LOG.debug("Couldn't find JSP output on that line");
      }
      return Optional.empty();
    }
  }

  @Override
  public WeavingResult visitRepositoryFile(
      final File repositoryRoot,
      final File file,
      final CodemodChangeRecorder weavingContext,
      final Set<ChangedFile> changedJavaFiles) {
    Set<String> unscannableFiles = Collections.emptySet();
    try {
      String path = file.getCanonicalPath().substring(repositoryRoot.getCanonicalPath().length());
      Set<Result> xssFileResults =
          xssResults.stream()
              .filter(
                  r ->
                      r.getLocations()
                          .get(0)
                          .getPhysicalLocation()
                          .getArtifactLocation()
                          .getUri()
                          .equals(path))
              .collect(Collectors.toSet());

      if (xssFileResults.isEmpty()) {
        return WeavingResult.empty();
      }

      Set<Integer> vulnerableLines =
          xssFileResults.stream()
              .map(r -> r.getLocations().get(0).getPhysicalLocation().getRegion().getStartLine())
              .collect(Collectors.toSet());

      List<String> lines = FileUtils.readLines(file);
      List<String> rebuiltLines = new ArrayList<>(lines.size());
      List<CodemodChange> weaves = new ArrayList<>();

      JspLineRewriterStrategy jspLineRewriterStrategy =
          new DefaultJspLineRewriterStrategy(
              Set.of(
                  new JspExpressionOutputMethod(),
                  new JspActionTagOutputMethod(),
                  new JspExpressionLanguageOutputMethod(lines)));

      Set<String> taglibsNeeded = new HashSet<>();
      for (int i = 0; i < lines.size(); i++) {
        String line = lines.get(i);
        int lineNumber = i + 1;
        if (vulnerableLines.contains(lineNumber)) {
          Set<Result> resultsForThisLine =
              xssFileResults.stream()
                  .filter(
                      r ->
                          lineNumber
                              == r.getLocations()
                                  .get(0)
                                  .getPhysicalLocation()
                                  .getRegion()
                                  .getStartLine())
                  .collect(Collectors.toSet());
          Optional<JspLineWeave> lineWeaveRef =
              jspLineRewriterStrategy.rewriteJspLine(resultsForThisLine, lines, line);
          if (lineWeaveRef.isPresent()) {
            JspLineWeave lineWeave = lineWeaveRef.get();
            String rebuiltLine = lineWeave.getRebuiltLine();
            rebuiltLines.add(rebuiltLine);
            DependencyGAV dependencyNeeded = lineWeave.getDependencyNeeded();
            weaves.add(
                CodemodChange.from(
                    lineNumber,
                    lineWeave.getRuleId(),
                    dependencyNeeded != null
                        ? List.of(dependencyNeeded)
                        : Collections.emptyList()));
            Optional<String> supportingTaglibRef = lineWeave.getSupportingTaglib();
            supportingTaglibRef.ifPresent(taglibsNeeded::add);
          } else {
            rebuiltLines.add(line);
          }
        } else {
          rebuiltLines.add(line);
        }
      }

      if (!weaves.isEmpty()) {
        for (final String taglib : taglibsNeeded) {
          rebuiltLines.add(0, taglib);
        }
        File tmpFile = File.createTempFile("xss-rewrite", ".jsp");
        FileUtils.writeLines(tmpFile, rebuiltLines);
        ChangedFile changedFile =
            ChangedFile.createDefault(file.getAbsolutePath(), tmpFile.getAbsolutePath(), weaves);
        return WeavingResult.createDefault(Set.of(changedFile), Collections.emptySet());
      }

    } catch (IOException e) {
      LOG.error("Problem weaving JSP XSS results {}", file, e);
      unscannableFiles =
          Set.of(file.getAbsolutePath().substring(repositoryRoot.getAbsolutePath().length()));
    }
    return WeavingResult.createDefault(Collections.emptySet(), unscannableFiles);
  }

  private static final Logger LOG = LoggerFactory.getLogger(SarifBasedJspScriptletXSSVisitor.class);
}
