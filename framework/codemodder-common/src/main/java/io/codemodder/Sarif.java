package io.codemodder;

import com.contrastsecurity.sarif.PhysicalLocation;
import com.contrastsecurity.sarif.Region;
import com.contrastsecurity.sarif.Result;
import com.contrastsecurity.sarif.ThreadFlowLocation;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Utility class for working with SARIF model objects. */
public final class Sarif {

  private Sarif() {}

  /**
   * Return true iif the {@link PhysicalLocation} represents the file passed in from the given
   * repository root.
   */
  public static boolean looksTheSame(
      final PhysicalLocation physicalLocation, final File repositoryRoot, final File file)
      throws IOException {
    String filePath = file.getCanonicalPath();
    String fileUri = physicalLocation.getArtifactLocation().getUri();
    return filePath.startsWith(repositoryRoot.getCanonicalPath()) && filePath.endsWith(fileUri);
  }

  /** Return all the results where the first location matches the URI given. */
  public static Set<Result> getAllResultsByFile(final Set<Result> results, final String uri) {
    return results.stream()
        .filter(r -> !r.getLocations().isEmpty())
        .filter(
            r ->
                r.getLocations()
                    .get(0)
                    .getPhysicalLocation()
                    .getArtifactLocation()
                    .getUri()
                    .equals(uri))
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * Return the first {@link Result} that has the given line number and token in the snippet text
   * for the first location.
   */
  public static Optional<Result> getFirstMatchingResult(
      final Set<Result> results, final int startLine, final String snippetToken) {
    return results.stream()
        .filter(
            result ->
                result.getLocations().get(0).getPhysicalLocation().getRegion().getStartLine()
                    == startLine)
        .filter(
            result ->
                result
                    .getLocations()
                    .get(0)
                    .getPhysicalLocation()
                    .getRegion()
                    .getSnippet()
                    .getText()
                    .contains(snippetToken))
        .findFirst();
  }

  /**
   * This method returns the last data flow location in the first code flow's first thread flow.
   * This assumes that the the SARIF tool models their data flow in this way. This appears true for
   * now.
   *
   * @param result the SARIF result
   * @return the last data flow location in the first code flow's first thread flow
   */
  public static Region getLastDataFlowRegion(final Result result) {
    List<ThreadFlowLocation> threadFlow =
        result.getCodeFlows().get(0).getThreadFlows().get(0).getLocations();
    PhysicalLocation lastLocation =
        threadFlow.get(threadFlow.size() - 1).getLocation().getPhysicalLocation();
    return lastLocation.getRegion();
  }
}
