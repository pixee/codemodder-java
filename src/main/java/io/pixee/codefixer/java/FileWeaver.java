package io.pixee.codefixer.java;

import static java.nio.file.Files.walk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This type is responsible for performing all the weaves that are not run on traditional Java
 * source code. It is meant to operate XML, JSP, and everything else that's not better handled by
 * {@link VisitorFactory}.
 */
interface FileWeaver {

  WeavingResult weave(
      List<FileBasedVisitor> repositoryWideWeavers,
      File repositoryRoot,
      WeavingResult javaSourceWeaveResult,
      IncludesExcludes includesExcludes);

  static FileWeaver createDefault() {
    return new Default();
  }

  class Default implements FileWeaver {

    @Override
    public WeavingResult weave(
        final List<FileBasedVisitor> repositoryWideWeavers,
        final File repositoryRoot,
        final WeavingResult javaSourceWeaveResult,
        final IncludesExcludes includesExcludes) {
      var changedFiles = new HashSet<ChangedFile>();
      var unscannableFiles = new HashSet<String>();
      repositoryWideWeavers.forEach(
          fileBasedWeaver -> {
            Path repositoryRootPath = repositoryRoot.toPath();
            try (Stream<Path> stream = walk(repositoryRootPath, Integer.MAX_VALUE)) {
              List<File> files = stream.map(Path::toFile).sorted().collect(Collectors.toList());
              for (File filePath : files) {
                var canonicalFile = filePath.getCanonicalFile();
                var context =
                    new FileWeavingContext.Default(
                        canonicalFile, includesExcludes.getIncludesExcludesForFile(filePath));
                WeavingResult result =
                    fileBasedWeaver.visitRepositoryFile(
                        repositoryRoot,
                        canonicalFile,
                        context,
                        javaSourceWeaveResult.changedFiles());
                changedFiles.addAll(result.changedFiles());
                unscannableFiles.addAll(result.unscannableFiles());
              }
            } catch (IOException e) {
              LOG.error("Problem scanning repository files", e);
            }
          });
      return WeavingResult.createDefault(changedFiles, unscannableFiles);
    }

    private static final Logger LOG = LoggerFactory.getLogger(Default.class);
  }
}
