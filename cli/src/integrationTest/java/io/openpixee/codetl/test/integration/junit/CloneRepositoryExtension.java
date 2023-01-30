package io.openpixee.codetl.test.integration.junit;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.util.stream.Collectors.joining;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.extension.ExtensionConfigurationException;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store.CloseableResource;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * JUnit extension that clones remote git repositories, and cleans up all such repositories at the
 * conclusion of the test suite. Repositories are clean-up at the conclusion of the test suite, so
 * that repositories may be reused across tests.
 */
public final class CloneRepositoryExtension implements ParameterResolver {

  @Override
  public boolean supportsParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return Path.class.isAssignableFrom(parameterContext.getParameter().getType())
        && parameterContext.isAnnotated(CloneRepository.class);
  }

  @Override
  public Object resolveParameter(
      final ParameterContext parameterContext, final ExtensionContext extensionContext)
      throws ParameterResolutionException {
    final Path root = root(extensionContext);
    final CloneRepository annotation =
        parameterContext
            .findAnnotation(CloneRepository.class)
            .orElseThrow(
                () ->
                    new NullPointerException(
                        "failed to discover required GitRepository annotation"));
    return clone(root, annotation);
  }

  private Object clone(final Path root, final CloneRepository annotation) {
    final String repo = annotation.repo();
    final URI repoURI = URI.create(repo);
    final String[] paths = repoURI.getPath().split("/");
    final String branchDirectoryName =
        URLEncoder.encode(annotation.branch(), StandardCharsets.UTF_8);
    final Path repoDirectory = root.resolve(paths[paths.length - 1]);
    try {
      Files.createDirectory(repoDirectory);
    } catch (FileAlreadyExistsException ignored) {
    } catch (IOException e) {
      throw new ParameterResolutionException(
          "Failed to create repository directory " + repoDirectory, e);
    }
    final Path branchDirectory = repoDirectory.resolve(branchDirectoryName);
    // TODO tests will not be able to run in parallel until we have some synchronization around this
    // cloning
    if (!Files.exists(branchDirectory)) {
      // it would be nice to do a shallow clone here, but jgit has no such functionality
      final CloneCommand clone =
          Git.cloneRepository()
              .setURI(repo)
              .setDirectory(branchDirectory.toFile())
              .setBranch(annotation.branch());
      final Git git;
      try {
        git = clone.call();
      } catch (GitAPIException e) {
        throw new ParameterResolutionException("Failed to clone " + repo, e);
      }
      git.close();
    }
    return branchDirectory;
  }

  private Path root(final ExtensionContext extensionContext) {
    return extensionContext
        .getRoot()
        .getStore(NAMESPACE)
        .getOrComputeIfAbsent(KEY, __ -> createTempDir(), CloseablePath.class)
        .get();
  }

  private static CloseablePath createTempDir() {
    try {
      return new CloseablePath(Files.createTempDirectory(TEMP_DIR_PREFIX));
    } catch (Exception ex) {
      throw new ExtensionConfigurationException("Failed to create default temp directory", ex);
    }
  }

  private record CloseablePath(Path dir) implements CloseableResource {

    Path get() {
      return dir;
    }

    @Override
    public void close() throws IOException {
      SortedMap<Path, IOException> failures = deleteAllFilesAndDirectories();
      if (!failures.isEmpty()) {
        throw createIOExceptionWithAttachedFailures(failures);
      }
    }

    private SortedMap<Path, IOException> deleteAllFilesAndDirectories() throws IOException {
      if (Files.notExists(dir)) {
        return Collections.emptySortedMap();
      }

      SortedMap<Path, IOException> failures = new TreeMap<>();
      resetPermissions(dir);
      Files.walkFileTree(
          dir,
          new SimpleFileVisitor<>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
              if (!dir.equals(CloseablePath.this.dir)) {
                try {
                  resetPermissions(dir);
                } catch (IOException ignored) {
                }
              }
              return CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
              // IOException includes `AccessDeniedException` thrown by non-readable or
              // non-executable flags
              resetPermissionsAndTryToDeleteAgain(file, exc);
              return CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
              return deleteAndContinue(file);
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
              return deleteAndContinue(dir);
            }

            private FileVisitResult deleteAndContinue(Path path) {
              try {
                Files.delete(path);
              } catch (NoSuchFileException ignore) {
                // ignore
              } catch (DirectoryNotEmptyException exception) {
                failures.put(path, exception);
              } catch (IOException exception) {
                // IOException includes `AccessDeniedException` thrown by non-readable or
                // non-executable flags
                resetPermissionsAndTryToDeleteAgain(path, exception);
              }
              return CONTINUE;
            }

            private void resetPermissionsAndTryToDeleteAgain(Path path, IOException exception) {
              try {
                resetPermissions(path);
                if (Files.isDirectory(path)) {
                  Files.walkFileTree(path, this);
                } else {
                  Files.delete(path);
                }
              } catch (Exception suppressed) {
                exception.addSuppressed(suppressed);
                failures.put(path, exception);
              }
            }
          });
      return failures;
    }

    private static void resetPermissions(Path path) throws IOException {
      final var permissions = Files.getPosixFilePermissions(path);
      permissions.add(PosixFilePermission.OWNER_READ);
      permissions.add(PosixFilePermission.OWNER_WRITE);
      if (Files.isDirectory(path)) {
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
      }
      Files.setPosixFilePermissions(path, permissions);
    }

    private IOException createIOExceptionWithAttachedFailures(
        SortedMap<Path, IOException> failures) {
      // @formatter:off
      String joinedPaths =
          failures.keySet().stream()
              .map(this::tryToDeleteOnExit)
              .map(this::relativizeSafely)
              .map(String::valueOf)
              .collect(joining(", "));
      // @formatter:on
      IOException exception =
          new IOException(
              "Failed to delete temp directory "
                  + dir.toAbsolutePath()
                  + ". The following paths could not be deleted (see suppressed exceptions for details): "
                  + joinedPaths);
      failures.values().forEach(exception::addSuppressed);
      return exception;
    }

    private Path tryToDeleteOnExit(Path path) {
      try {
        path.toFile().deleteOnExit();
      } catch (UnsupportedOperationException ignore) {
      }
      return path;
    }

    private Path relativizeSafely(Path path) {
      try {
        return dir.relativize(path);
      } catch (IllegalArgumentException e) {
        return path;
      }
    }
  }

  private static final Namespace NAMESPACE = Namespace.create(CloneRepositoryExtension.class);
  private static final String KEY = "git-repository-dir";
  private static final String TEMP_DIR_PREFIX = "junit-git-repo";
}
