package io.codemodder;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

final class DefaultCodeDirectory implements CodeDirectory {

  private final Path repositoryDir;

  DefaultCodeDirectory(final Path repositoryDir) {
    if (!Files.exists(repositoryDir)) {
      throw new IllegalArgumentException("code directory doesn't exist");
    }
    if (!Files.isDirectory(repositoryDir)) {
      throw new IllegalArgumentException("code directory isn't a directory");
    }
    if (!Files.isReadable(repositoryDir)) {
      throw new IllegalArgumentException("code directory isn't readable");
    }

    this.repositoryDir = Objects.requireNonNull(repositoryDir);
  }

  @Override
  public Path asPath() {
    return repositoryDir;
  }

  @Override
  public Optional<Path> findFilesWithTrailingPath(final String path) throws IOException {
    // find the files with the trailing path
    AtomicReference<Path> found = new AtomicReference<>();
    Files.walkFileTree(
        repositoryDir,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) {
            if (file.toString().endsWith(path)) {
              found.set(file);
              return FileVisitResult.TERMINATE;
            }
            return FileVisitResult.CONTINUE;
          }
        });
    return Optional.ofNullable(found.get());
  }
}
