package io.codemodder;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a worker class that reads a list of repository roots and captures the Java source
 * directories.
 */
public interface SourceDirectoryLister {

  List<SourceDirectory> listJavaSourceDirectories(List<File> directories) throws IOException;

  final class DefaultSourceDirectoryLister implements SourceDirectoryLister {

    /**
     * This method turns the list of source code roots into resolved Java source code directories,
     * with all the Java source files.
     */
    @Override
    public List<SourceDirectory> listJavaSourceDirectories(final List<File> directories)
        throws IOException {
      final List<SourceDirectory> javaSourceDirectories = new ArrayList<>();
      var javaSourceDir = "src" + File.separatorChar + "main" + File.separatorChar + "java";
      for (File directory : directories) {
        List<Path> javaSourceDirectoryPaths =
            getSourceDirectoryPathsWithSuffix(directory, javaSourceDir);
        List<SourceDirectory> sourceDirectories =
            convertToSourceDirectories(javaSourceDirectoryPaths);
        javaSourceDirectories.addAll(sourceDirectories);
      }
      return javaSourceDirectories;
    }

    @NotNull
    private List<SourceDirectory> convertToSourceDirectories(final List<Path> sourceDirectoryPaths)
        throws IOException {
      List<SourceDirectory> sourceDirectories = new ArrayList<>();
      for (final Path sourceDirectoryPath : sourceDirectoryPaths) {
        List<String> javaFilePaths =
            Files.walk(sourceDirectoryPath)
                .filter(Files::isRegularFile)
                .map(path -> path.toAbsolutePath().toString())
                .filter(path -> path.toLowerCase().endsWith(".java"))
                .filter(path -> !path.toLowerCase().contains(testDirToken))
                .collect(Collectors.toUnmodifiableList());

        sourceDirectories.add(
            SourceDirectory.createDefault(
                sourceDirectoryPath.toAbsolutePath().toString(), javaFilePaths));
      }
      return Collections.unmodifiableList(sourceDirectories);
    }

    @NotNull
    private List<Path> getSourceDirectoryPathsWithSuffix(
        final File directory, final String pathSuffixDesired) throws IOException {
      final String canonicalPathSuffixDesired = pathSuffixDesired.toLowerCase();
      return Files.walk(directory.toPath())
          .filter(Files::isDirectory)
          .filter(SourceDirectoryLister::isNotHiddenDirectory)
          .filter(dir -> !dir.getFileName().startsWith("."))
          .filter(
              dir ->
                  dir.toAbsolutePath()
                      .toString()
                      .toLowerCase()
                      .endsWith(canonicalPathSuffixDesired))
          .collect(Collectors.toUnmodifiableList());
    }

    private static final String testDirToken =
        "test" + File.separatorChar + "java" + File.separatorChar;
  }

  static SourceDirectoryLister createDefault() {
    return new DefaultSourceDirectoryLister();
  }

  /** Return true if the directory is not "hidden". */
  private static boolean isNotHiddenDirectory(final Path path) {
    return !path.getFileName().startsWith(".");
  }
}
