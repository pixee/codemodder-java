package io.pixee.codefixer.java;

import com.github.difflib.DiffUtils;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import com.google.common.annotations.VisibleForTesting;
import io.github.pixee.ccf.CCFChange;
import io.github.pixee.ccf.CCFConfiguration;
import io.github.pixee.ccf.CCFFileExtensionScanned;
import io.github.pixee.ccf.CCFReport;
import io.github.pixee.ccf.CCFResult;
import io.github.pixee.ccf.CCFRun;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

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

/**
 * This type is responsible for generating a {@link CCFReport} based on the domain objects representing our inputs and
 * outputs.
 */
interface CcfReportGenerator {

    /**
     * Creates the report.
     *
     * @param repositoryRoot the root directory of the repository
     * @param includePatterns the include patterns the user specified in configuration
     * @param excludePatterns the exclude patterns the user specified in configuration
     * @param weaveResults the combined results from all of our analysis
     * @param millisElapsed how long the scan/fix took
     *
     * @return the {@link CCFReport} representing all the stuff above
     */
    CCFReport createReport(File repositoryRoot, List<String> includePatterns, List<String> excludePatterns, WeavingResult weaveResults, long millisElapsed) throws IOException;

    static CcfReportGenerator createDefault() {
        return new Default();
    }

    class Default implements CcfReportGenerator {
        @Override
        public CCFReport createReport(final File repositoryRoot, final List<String> includePatterns, final List<String> excludePatterns, final WeavingResult allWeaveResults, final long elapsed) throws IOException {

            CCFConfiguration ccfConfiguration =
                    new CCFConfiguration(
                            repositoryRoot.getAbsolutePath(),
                            includePatterns,
                            excludePatterns,
                            Collections.emptyList(),
                            Collections.emptyList());

            CCFRun run =
                    new CCFRun(
                            "pixee",
                            "pixee-java",
                            null,
                            elapsed,
                            getFilesScanned(repositoryRoot),
                            ccfConfiguration,
                            List.copyOf(allWeaveResults.unscannableFiles()));

            List<CCFResult> results;
            try {
                results = toCcfResults(allWeaveResults, repositoryRoot.getCanonicalPath());
            } catch (PatchFailedException e) {
                throw new IOException("failed to generate patch", e);
            }
            return new CCFReport(run, results);
        }

        @VisibleForTesting
        List<CCFFileExtensionScanned> getFilesScanned(final File repositoryRoot) throws IOException {
            Map<String, Integer> extensionScannedMap = new HashMap<>();

            try (Stream<Path> walkStream = Files.walk(repositoryRoot.toPath())) {
                walkStream
                        .filter(p -> p.toFile().isFile())
                        .forEach(
                                f -> {
                                    String name = f.toString();
                                    String ext = FilenameUtils.getExtension(name);
                                    if (!StringUtils.isBlank(ext)) {
                                        Integer count = extensionScannedMap.computeIfAbsent(ext.toLowerCase(), (k) -> 0);
                                        count++;
                                        extensionScannedMap.put(ext.toLowerCase(), count);
                                    }
                                });
            }
            return extensionScannedMap.entrySet().stream()
                    .map(ext -> new CCFFileExtensionScanned(ext.getKey(), ext.getValue()))
                    .collect(Collectors.toUnmodifiableList());
        }

        /** Turn the results from our format into {@link CCFResult}. */
        private List<CCFResult> toCcfResults(final WeavingResult results, final String repositoryRootPath)
                throws IOException, PatchFailedException {
            List<CCFResult> ccfResults = new ArrayList<>();
            Set<ChangedFile> changedFiles = results.changedFiles();
            for (ChangedFile changedFile : changedFiles) {
                String originalFilePath = changedFile.originalFilePath();
                String newFile = changedFile.modifiedFile();
                File originalFile = new File(originalFilePath);
                List<String> original = Files.readAllLines(originalFile.toPath());
                List<String> patched = Files.readAllLines(new File(newFile).toPath());
                Patch<String> patch = DiffUtils.diff(original, patched);
                String path = originalFilePath.substring(repositoryRootPath.length());
                if(path.startsWith("/")) {
                    path = path.substring(1);
                }

                String diff =
                        String.join("\n", UnifiedDiffUtils.generateUnifiedDiff(path, path, original, patch, 0));
                List<Weave> weaves = changedFile.weaves();
                List<CCFChange> changes =
                        weaves.stream()
                                .map(
                                        weave ->
                                                new CCFChange(
                                                        weave.lineNumber(),
                                                        Collections.emptyMap(),
                                                        weave.changeCode(),
                                                        "fill description for " + weave.changeCode()))
                                .collect(Collectors.toList());
                CCFResult ccfResult = new CCFResult(path, diff, changes);
                ccfResults.add(ccfResult);
            }
            return Collections.unmodifiableList(ccfResults);
        }
    }
}
