package io.openpixee.java;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.google.common.annotations.VisibleForTesting;
import io.github.pixee.codetf.CodeTFChange;
import io.github.pixee.codetf.CodeTFConfiguration;
import io.github.pixee.codetf.CodeTFFileExtensionScanned;
import io.github.pixee.codetf.CodeTFReport;
import io.github.pixee.codetf.CodeTFResult;
import io.github.pixee.codetf.CodeTFRun;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * This type is responsible for generating a {@link CodeTFReport} based on the domain objects
 * representing our inputs and outputs.
 */
interface CodeTFReportGenerator {

  /**
   * Creates the report.
   *
   * @param repositoryRoot the root directory of the repository
   * @param includePatterns the include patterns the user specified in configuration
   * @param excludePatterns the exclude patterns the user specified in configuration
   * @param weaveResults the combined results from all of our analysis
   * @param millisElapsed how long the scan/fix took
   * @return the {@link CodeTFReport} representing all the stuff above
   */
  CodeTFReport createReport(
      File repositoryRoot,
      List<String> includePatterns,
      List<String> excludePatterns,
      WeavingResult weaveResults,
      long millisElapsed)
      throws IOException;

  static CodeTFReportGenerator createDefault() {
    return new Default();
  }

  class Default implements CodeTFReportGenerator {
    @Override
    public CodeTFReport createReport(
        final File repositoryRoot,
        final List<String> includePatterns,
        final List<String> excludePatterns,
        final WeavingResult allWeaveResults,
        final long elapsed)
        throws IOException {

      CodeTFConfiguration configuration =
          new CodeTFConfiguration(
              repositoryRoot.getAbsolutePath(),
              includePatterns,
              excludePatterns,
              Collections.emptyList(),
              Collections.emptyList());

      CodeTFRun run =
          new CodeTFRun(
              "pixee",
              "pixee-java",
              null,
              elapsed,
              getFilesScanned(repositoryRoot),
              configuration,
              List.copyOf(allWeaveResults.unscannableFiles()));

      List<CodeTFResult> results;
      try {
        results = toCodeTFResults(allWeaveResults, repositoryRoot.getCanonicalPath());
      } catch (PatchFailedException e) {
        throw new IOException("failed to generate patch", e);
      }
      return new CodeTFReport(run, results);
    }

    @VisibleForTesting
    List<CodeTFFileExtensionScanned> getFilesScanned(final File repositoryRoot) throws IOException {
      Map<String, Integer> extensionScannedMap = new HashMap<>();

      try (Stream<Path> walkStream = Files.walk(repositoryRoot.toPath())) {
        walkStream
            .filter(p -> p.toFile().isFile())
            .forEach(
                f -> {
                  String name = f.toString();
                  String ext = FilenameUtils.getExtension(name);
                  if (!StringUtils.isBlank(ext)) {
                    Integer count =
                        extensionScannedMap.computeIfAbsent(ext.toLowerCase(), (k) -> 0);
                    count++;
                    extensionScannedMap.put(ext.toLowerCase(), count);
                  }
                });
      }
      return extensionScannedMap.entrySet().stream()
          .map(ext -> new CodeTFFileExtensionScanned(ext.getKey(), ext.getValue()))
          .collect(Collectors.toUnmodifiableList());
    }

    /** Turn the results from our format into {@link CodeTFResult}. */
    private List<CodeTFResult> toCodeTFResults(
        final WeavingResult results, final String repositoryRootPath)
        throws IOException, PatchFailedException {
      List<CodeTFResult> codetfResults = new ArrayList<>();
      Set<ChangedFile> changedFiles = results.changedFiles();
      for (ChangedFile changedFile : changedFiles) {
        String originalFilePath = changedFile.originalFilePath();
        String newFile = changedFile.modifiedFile();
        File originalFile = new File(originalFilePath);
        List<String> original = Files.readAllLines(originalFile.toPath());
        List<String> patched = Files.readAllLines(new File(newFile).toPath());
        Patch<String> patch = DiffUtils.diff(original, patched);
        String path = originalFilePath.substring(repositoryRootPath.length());
        if (path.startsWith("/")) {
          path = path.substring(1);
        }

        String diff =
            String.join("\n", UnifiedDiffUtils.generateUnifiedDiff(path, path, original, patch, 0));
        List<Weave> weaves = changedFile.weaves();
        List<CodeTFChange> changes =
            weaves.stream()
                .map(
                    weave ->
                        new CodeTFChange(
                            weave.lineNumber(),
                            Collections.emptyMap(),
                            weave.changeCode(),
                            "fill description for " + weave.changeCode()))
                .collect(Collectors.toList());

        if (StringUtils.isNotBlank(diff)) {
          CodeTFResult result = new CodeTFResult(path, diff, changes);
          codetfResults.add(result);
        }
      }
      return Collections.unmodifiableList(codetfResults);
    }
  }
}
