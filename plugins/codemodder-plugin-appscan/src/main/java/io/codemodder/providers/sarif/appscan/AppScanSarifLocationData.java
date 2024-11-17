package io.codemodder.providers.sarif.appscan;

import com.contrastsecurity.sarif.Artifact;
import com.contrastsecurity.sarif.ArtifactLocation;
import com.contrastsecurity.sarif.SarifSchema210;
import io.codemodder.CodeDirectory;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Holds the locations of the artifacts in the SARIF file. */
final class AppScanSarifLocationData {

  private final Map<Path, Set<Integer>> artifactLocationIndices;

  AppScanSarifLocationData(final SarifSchema210 sarif, final CodeDirectory codeDirectory) {
    log.debug("Cleaning locations");
    List<String> locations =
        sarif.getRuns().get(0).getArtifacts().stream()
            .map(Artifact::getLocation)
            .map(ArtifactLocation::getUri)
            .map(u -> u.substring(8)) // trim the file:/// of all results
            .toList();

    Map<Path, Set<Integer>> artifactLocationIndicesMap = new HashMap<>();

    log.info("Calculating locations in project dir");
    for (int i = 0; i < locations.size(); i++) {
      final Integer index = i;
      String path = locations.get(i);
      path = path.replace('\\', '/');
      // we have a real but partial path, now we have to find it in the repository
      Optional<Path> existingRealPath;
      try {
        existingRealPath = codeDirectory.findFilesWithTrailingPath(path);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
      // add to the map if we found a matching file
      existingRealPath.ifPresent(
          p -> artifactLocationIndicesMap.computeIfAbsent(p, k -> new HashSet<>()).add(index));
    }
    log.info("Done calculating locations");
    this.artifactLocationIndices = Map.copyOf(artifactLocationIndicesMap);
  }

  Map<Path, Set<Integer>> getArtifactLocationIndices() {
    return artifactLocationIndices;
  }

  private static final Logger log = LoggerFactory.getLogger(AppScanSarifLocationData.class);
}
