package io.codemodder.plugins.jpms;

import io.codemodder.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Provides JPMS (Java Platform Module System) management functions to codemods. */
public final class JpmsProvider implements ProjectProvider {

  private final ModuleInfoUpdater moduleInfoUpdater;

  public JpmsProvider() {
    this(new DefaultModuleInfoUpdater());
  }

  private JpmsProvider(final ModuleInfoUpdater moduleInfoUpdater) {
    this.moduleInfoUpdater = moduleInfoUpdater;
  }

  @Override
  public DependencyUpdateResult updateDependencies(
      final Path projectDir, final Path file, final List<DependencyGAV> remainingFileDependencies)
      throws IOException {

    Optional<Path> moduleInfoJava = findModuleInfoJava(projectDir, file);

    DependencyUpdateResult result = DependencyUpdateResult.EMPTY_UPDATE;
    if (moduleInfoJava.isEmpty()) {
      return result;
    }

    if (remainingFileDependencies.stream()
        .allMatch(dependency -> dependency.moduleName().isEmpty())) {
      LOG.debug("No dependencies with module names. Can't inject JPMS provider.");
      return result;
    }

    try {
      result =
          moduleInfoUpdater.update(
              projectDir, moduleInfoJava.get(), file, remainingFileDependencies);
    } catch (IOException e) {
      LOG.error("Problem updating module-info.java", e);
    }
    return result;
  }

  private Optional<Path> findModuleInfoJava(final Path projectDir, final Path file)
      throws IOException {
    // start with th
    Path parent = file;
    while (parent != null && !Files.isSameFile(parent, projectDir)) {
      // if we're in a src/main/java dir, check for the presence of `module-info.java`
      Path moduleInfoJava = parent.resolve("module-info.java");
      if (Files.exists(moduleInfoJava) && Files.isRegularFile(moduleInfoJava)) {
        return Optional.of(moduleInfoJava);
      }
      parent = parent.getParent();
    }
    return Optional.empty();
  }

  private static final Logger LOG = LoggerFactory.getLogger(JpmsProvider.class);
}
