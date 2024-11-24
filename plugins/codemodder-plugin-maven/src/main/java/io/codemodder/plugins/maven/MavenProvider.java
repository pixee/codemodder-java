package io.codemodder.plugins.maven;

import io.codemodder.*;
import io.codemodder.plugins.maven.operator.POMOperator;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.jetbrains.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides Maven dependency management functions to codemods.
 *
 * <p>Current Limitations are:
 *
 * <p>a. We skip parent finding if there's not a relativePath declaration (this is by design), so
 * sometimes pom finding will fail on purpose b. there are several flags on ProjectModelFactory
 * which aren't applied. They relate to verisons, upgrading and particularly: Actives Profiles c. If
 * you need anything declared in a ~/.m2/settings.xml, we don't support that (e.g., passwords or
 * proxies) d. Haven't tested, but I'm almost sure that it wouldn't work on any repo other than
 * central e. We allow on this module to do online resolution. HOWEVER by default its offline f. You
 * need to set an `M2_REPO` environment variable and/or property or declare withRepositoryPath to
 * somewhere writable
 */
public final class MavenProvider implements ProjectProvider {

  /** Represents a failure when doing a dependency update. */
  static class DependencyUpdateException extends RuntimeException {
    DependencyUpdateException(String message, Throwable cause) {
      super(message, cause);
    }
  }

  /** A seam for handling writing poms to disk. */
  interface PomModifier {
    /**
     * Modifies a POM writing back its contents
     *
     * @param path where to write
     * @param contents contents to write
     * @throws IOException failure when writing
     */
    void modify(final Path path, final byte[] contents) throws IOException;
  }

  /** Default Implementation of Pom Modifier Interface */
  static class DefaultPomModifier implements PomModifier {
    @Override
    public void modify(final Path path, final byte[] contents) throws IOException {
      Files.write(path, contents);
    }
  }

  private final POMDependencyUpdater pomDependencyUpdater;
  private final PomFileFinder pomFileFinder;

  MavenProvider(
      final PomFileFinder pomFileFinder, final POMDependencyUpdater pomDependencyUpdater) {
    this.pomFileFinder = Objects.requireNonNull(pomFileFinder);
    this.pomDependencyUpdater = Objects.requireNonNull(pomDependencyUpdater);
  }

  MavenProvider(
      final PomModifier pomModifier,
      final PomFileFinder pomFileFinder,
      final DependencyDescriptor dependencyDescriptor,
      final ArtifactInjectionPositionFinder positionFinder) {
    this(
        pomFileFinder,
        new DefaultPOMDependencyUpdater(
            new CodeTFGenerator(positionFinder, dependencyDescriptor), pomFileFinder, pomModifier));
  }

  MavenProvider(
      final PomModifier pomModifier,
      final PomFileFinder pomFileFinder,
      final DependencyDescriptor dependencyDescriptor) {
    this(
        pomModifier,
        pomFileFinder,
        dependencyDescriptor,
        new DefaultArtifactInjectionPositionFinder());
  }

  MavenProvider(final PomModifier pomModifier) {
    this(
        pomModifier,
        new DefaultPomFileFinder(),
        DependencyDescriptor.createMarkdownDescriptor(),
        new DefaultArtifactInjectionPositionFinder());
  }

  public MavenProvider() {
    this(
        new DefaultPomModifier(),
        new DefaultPomFileFinder(),
        DependencyDescriptor.createMarkdownDescriptor(),
        new DefaultArtifactInjectionPositionFinder());
  }

  @VisibleForTesting
  static class DefaultPomFileFinder implements PomFileFinder {
    @Override
    public Optional<Path> findForFile(final Path projectDir, final Path file) throws IOException {
      Path parent = file.getParent();
      while (parent != null && !Files.isSameFile(projectDir.getParent(), parent)) {
        Path pomPath = parent.resolve("pom.xml");
        if (Files.exists(pomPath)) {
          return Optional.of(pomPath);
        }
        parent = parent.getParent();
      }
      return Optional.empty();
    }
  }

  /**
   * This method must not throw exception -- it should capture failures in its model and bubble up
   * normal results.
   */
  @Override
  public DependencyUpdateResult updateDependencies(
      final Path projectDir, final Path file, final List<DependencyGAV> dependencies) {
    String dependenciesStr =
        dependencies.stream().map(DependencyGAV::toString).collect(Collectors.joining(","));
    LOG.trace("Updating dependencies for {} in {}: {}", file, projectDir, dependenciesStr);
    return pomDependencyUpdater.execute(projectDir, file, dependencies);
  }

  @Override
  public Collection<DependencyGAV> getAllDependencies(final Path projectDir, final Path file) {
    try {
      final Optional<Path> maybePomFile = pomFileFinder.findForFile(projectDir, file);
      if (maybePomFile.isEmpty()) {
        LOG.trace("Pom file was empty for {}", file);
        return Collections.emptyList();
      }

      final Path pomFile = maybePomFile.get();
      final POMOperator pomOperator = new POMOperator(pomFile, projectDir);

      return pomOperator.getAllFoundDependencies();
    } catch (Exception e) {
      LOG.warn("Not all Maven dependencies could be found", e);
    }
    return Collections.emptyList();
  }

  private static final Logger LOG = LoggerFactory.getLogger(MavenProvider.class);
}
