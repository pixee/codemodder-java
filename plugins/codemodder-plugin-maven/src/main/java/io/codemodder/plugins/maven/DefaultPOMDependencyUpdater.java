package io.codemodder.plugins.maven;

import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import io.codemodder.codetf.CodeTFChangesetEntry;
import io.codemodder.plugins.maven.operator.POMDocument;
import io.codemodder.plugins.maven.operator.POMOperator;
import io.codemodder.plugins.maven.operator.ProjectModel;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import javax.xml.stream.XMLStreamException;
import org.dom4j.DocumentException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** POMDependencyUpdater is responsible for updating Maven POM files with new dependencies. */
final class DefaultPOMDependencyUpdater implements POMDependencyUpdater {
  private final PomFileFinder pomFileFinder;

  private Optional<Path> maybePomFile;

  private final MavenProvider.PomModifier pomModifier;

  private final CodeTFGenerator codeTFGenerator;

  private List<CodeTFChangesetEntry> changesets;

  private List<DependencyGAV> skippedDependencies;
  private List<DependencyGAV> injectedDependencies;
  private Set<Path> erroredFiles;

  private AtomicReference<Collection<DependencyGAV>> foundDependenciesMapped;

  private static final Logger LOG = LoggerFactory.getLogger(DefaultPOMDependencyUpdater.class);

  /**
   * Constructs a POMDependencyUpdater with the specified CodeTFGenerator, PomFileFinder, and
   * PomModifier.
   *
   * @param codeTFGenerator The CodeTFGenerator for generating CodeTFChangesetEntries.
   * @param pomFileFinder The PomFileFinder for locating POM files.
   * @param pomModifier The MavenProvider.PomModifier for modifying POM files.
   */
  DefaultPOMDependencyUpdater(
      final CodeTFGenerator codeTFGenerator,
      final PomFileFinder pomFileFinder,
      final MavenProvider.PomModifier pomModifier) {
    this.pomFileFinder = Objects.requireNonNull(pomFileFinder);
    this.pomModifier = Objects.requireNonNull(pomModifier);
    this.codeTFGenerator = Objects.requireNonNull(codeTFGenerator);
  }

  /**
   * Execute the dependency update process for a specific project directory and set of dependencies.
   *
   * @param projectDir The project directory where the POM files are located.
   * @param file The specific POM file to update.
   * @param dependencies The list of new dependencies to be added.
   * @return A DependencyUpdateResult containing information about the update process.
   */
  @NotNull
  public DependencyUpdateResult execute(
      final Path projectDir, final Path file, final List<DependencyGAV> dependencies) {

    // if we can't find a pom, we want to skip this file
    try {
      if (isEmptyPomFile(projectDir, file)) {
        LOG.trace("Pom file was empty for {}", file);
        return DependencyUpdateResult.EMPTY_UPDATE;
      }
    } catch (IOException e) {
      LOG.warn("Not all Maven dependencies could be found", e);
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    final Path pomFile = maybePomFile.get();
    final POMOperator pomOperator = new POMOperator(pomFile, projectDir);

    changesets = new ArrayList<>();
    skippedDependencies = new ArrayList<>();
    injectedDependencies = new ArrayList<>();
    erroredFiles = new LinkedHashSet<>();

    // get all the existing dependencies, if we're not able to find them, we can't update
    Collection<DependencyGAV> allFoundDependencies;
    try {
      allFoundDependencies = pomOperator.getAllFoundDependencies();
    } catch (IOException | DocumentException | XMLStreamException | URISyntaxException e) {
      LOG.warn("Problem calculating all dependencies", e);
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    foundDependenciesMapped = new AtomicReference<>(allFoundDependencies);
    LOG.trace("Beginning dependency set size: {}", foundDependenciesMapped.get().size());

    dependencies.forEach(
        newDependencyGAV -> {
          try {

            if (updateSkipDependencies(newDependencyGAV)) {
              LOG.trace("Found it -- skipping");
              return;
            }

            final ProjectModel modifiedProjectModel = pomOperator.addDependency(newDependencyGAV);
            if (modifiedProjectModel == null) {
              LOG.trace("POM file didn't need modification or it failed?");
              return;
            }

            LOG.trace("Modified the pom -- writing it back");

            modifyPomFiles(projectDir, modifiedProjectModel, newDependencyGAV);

            // recalculate the dependencies after our addition
            final Collection<DependencyGAV> newDependencySet =
                pomOperator.getAllFoundDependencies();
            LOG.trace("New dependency set size: {}", newDependencySet.size());
            foundDependenciesMapped.set(newDependencySet);

          } catch (Exception e) {
            LOG.error("Problem  getting on pom operator", e);
          }
        });

    return DependencyUpdateResult.create(
        injectedDependencies, skippedDependencies, changesets, erroredFiles);
  }

  private boolean isEmptyPomFile(final Path projectDir, final Path file) throws IOException {
    maybePomFile = pomFileFinder.findForFile(projectDir, file);
    return maybePomFile.isEmpty();
  }

  private void modifyPomFiles(
      final Path projectDir,
      final ProjectModel modifiedProjectModel,
      final DependencyGAV newDependencyGAV) {
    Collection<POMDocument> allPomFiles = modifiedProjectModel.allPomFiles();
    LOG.trace("Found {} pom files -- {}", allPomFiles.size(), allPomFiles);

    for (POMDocument aPomFile : allPomFiles) {
      final URI uri = getPomFileURI(aPomFile);

      final Path path = Path.of(uri);

      modifyDirtyPomFile(projectDir, path, aPomFile, newDependencyGAV);
    }
  }

  private void modifyDirtyPomFile(
      final Path projectDir,
      final Path path,
      final POMDocument aPomFile,
      final DependencyGAV newDependencyGAV) {

    if (!aPomFile.getDirty()) {
      LOG.trace("POM file {} wasn't dirty", path);
      return;
    }

    LOG.trace("POM file {} was dirty", path);

    try {
      final CodeTFChangesetEntry entry =
          codeTFGenerator.getChanges(projectDir, aPomFile, newDependencyGAV);
      pomModifier.modify(path, aPomFile.getResultPomBytes());
      LOG.trace("POM written!");
      injectedDependencies.add(newDependencyGAV);
      changesets.add(entry);
    } catch (IOException | UncheckedIOException exc) {
      LOG.error("Failed to write pom", exc);
      erroredFiles.add(path);
    }
  }

  private URI getPomFileURI(final POMDocument aPomFile) {
    try {
      return aPomFile.getPomPath().toURI();
    } catch (URISyntaxException ex) {
      LOG.error("Unexpected problem getting pom URI", ex);
      throw new MavenProvider.DependencyUpdateException("Failure parsing URL: " + aPomFile, ex);
    }
  }

  private boolean updateSkipDependencies(final DependencyGAV newDependencyGAV) {
    LOG.trace("Looking at injecting new dependency: {}", newDependencyGAV);
    final boolean foundIt =
        foundDependenciesMapped.get().stream().anyMatch(newDependencyGAV::equals);

    if (foundIt) {
      skippedDependencies.add(newDependencyGAV);
      return true;
    }

    return false;
  }
}
