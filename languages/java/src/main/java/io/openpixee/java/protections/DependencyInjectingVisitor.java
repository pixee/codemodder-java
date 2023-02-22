package io.openpixee.java.protections;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;
import com.google.common.annotations.VisibleForTesting;
import io.openpixee.java.ChangedFile;
import io.openpixee.java.DependencyGAV;
import io.openpixee.java.FileBasedVisitor;
import io.openpixee.java.FileWeavingContext;
import io.openpixee.java.Weave;
import io.openpixee.java.WeavingResult;
import io.openpixee.maven.operator.POMOperator;
import io.openpixee.maven.operator.ProjectModel;
import io.openpixee.maven.operator.ProjectModelFactory;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an important default weaver that will inject the dependencies that the weaves require.
 */
public final class DependencyInjectingVisitor implements FileBasedVisitor {
  private final WeavingResult emptyWeaveResult;
  private boolean scanned;
  private Map<File, Set<DependencyGAV>> pomsToUpdate;

  public DependencyInjectingVisitor() {
    this.emptyWeaveResult =
        WeavingResult.createDefault(Collections.emptySet(), Collections.emptySet());
    this.scanned = false;
    this.pomsToUpdate = Collections.emptyMap();
  }

  @Override
  public String ruleId() {
    return pomInjectionRuleId;
  }

  @Override
  public WeavingResult visitRepositoryFile(
      final File repositoryRoot,
      final File file,
      final FileWeavingContext weavingContext,
      final Set<ChangedFile> changedJavaFiles) {

    if (!scanned) {
      scanned = true;
      try {
        this.pomsToUpdate = scan(repositoryRoot.getCanonicalFile(), changedJavaFiles);
      } catch (IOException e) {
        throw new IllegalArgumentException(e);
      }

      LOG.debug("Found the following poms to update:");
      pomsToUpdate.forEach(
          (pomFile, newDependencies) -> {
            LOG.debug("{} -> {}", pomFile, newDependencies);
          });
    }

    if (pomsToUpdate.containsKey(file)) {
      LOG.debug("Injecting dependency into: {}", file);
      try {
        var changedFile = transformPomIfNeeded(file, pomsToUpdate.get(file));
        if (changedFile != null) {
          return WeavingResult.createDefault(Set.of(changedFile), Collections.emptySet());
        }
      } catch (Exception e) {
        LOG.error("Problem injecting pom", e);
        return WeavingResult.createDefault(Collections.emptySet(), Set.of(file.getAbsolutePath()));
      }
    }
    return emptyWeaveResult;
  }

  private Map<File, Set<DependencyGAV>> scan(
      final File repositoryRoot, final Set<ChangedFile> changedJavaFiles) {
    LOG.debug(
        "Scanning repository root for all poms representing {} changed Java files",
        changedJavaFiles.size());
    final Map<File, Set<DependencyGAV>> pomDependencyUpdates = new HashMap<>();
    changedJavaFiles.forEach(
        changedFile -> {
          final var path = changedFile.originalFilePath();
          final var file = new File(path);
          final var aboveRepoDir = repositoryRoot.getParentFile().toPath();
          var parent = file.getParentFile();
          try {
            while (parent != null && !Files.isSameFile(parent.toPath(), aboveRepoDir)) {
              var potentialPom = new File(parent, "pom.xml");
              if (potentialPom.exists()) {
                LOG.debug("Adding pom: {}", potentialPom);
                Set<DependencyGAV> existingDependenciesForPom =
                    pomDependencyUpdates.computeIfAbsent(potentialPom, k -> new HashSet<>());
                Set<DependencyGAV> newDependenciesForPom =
                    changedFile.weaves().stream()
                        .flatMap(w -> w.getDependenciesNeeded().stream())
                        .collect(Collectors.toUnmodifiableSet());
                existingDependenciesForPom.addAll(newDependenciesForPom);
                parent = null;
              } else {
                parent = parent.getParentFile();
              }
            }
          } catch (IOException e) {
            LOG.error("Couldn't scan for pom files of changed Java source files", e);
          }
        });
    return pomDependencyUpdates;
  }

  @VisibleForTesting
  ChangedFile transformPomIfNeeded(final File file, final Set<DependencyGAV> dependenciesToAdd)
      throws IOException {
    /*
     * Short-circuit things
     */
    if (null == dependenciesToAdd || dependenciesToAdd.isEmpty()) {
      return null;
    }

    List<io.openpixee.maven.operator.Dependency> mappedDependencies =
        dependenciesToAdd.stream()
            .map(
                dependencyGAV ->
                    new io.openpixee.maven.operator.Dependency(
                        dependencyGAV.group(),
                        dependencyGAV.artifact(),
                        dependencyGAV.version(),
                        null,
                        null,
                        null))
            .collect(Collectors.toList());

    var originalPomContents =
        IOUtils.readLines(new FileInputStream(file), Charset.defaultCharset());

    final File lastPomFile = File.createTempFile("pom", ".xml");

    IOUtils.copy(new FileInputStream(file), new FileOutputStream(lastPomFile));

    mappedDependencies.forEach(
        newDependency -> {
          ProjectModel projectModel =
              ProjectModelFactory.load(lastPomFile)
                  .withDependency(newDependency)
                  .withOverrideIfAlreadyExists(true)
                  .withSkipIfNewer(true)
                  .withUseProperties(true)
                  .build();

          boolean result = POMOperator.modify(projectModel);

          if (result) {
            try {
              IOUtils.write(projectModel.getResultPomBytes(), new FileOutputStream(lastPomFile));
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }
        });

    var finalPomContents =
        IOUtils.readLines(new FileInputStream(lastPomFile), Charset.defaultCharset());

    if (finalPomContents.equals(originalPomContents)) {
      return null;
    }

    Patch<String> patch = DiffUtils.diff(originalPomContents, finalPomContents);

    AbstractDelta<String> delta = patch.getDeltas().get(0);
    int position = 1 + delta.getSource().getPosition();

    return ChangedFile.createDefault(
        file.getAbsolutePath(),
        lastPomFile.getAbsolutePath(),
        Weave.from(position, pomInjectionRuleId));
  }

  @VisibleForTesting static final String pomInjectionRuleId = "pixee:java/mvn-dependency-injection";
  @VisibleForTesting static final String projectArtifactId = "java-code-security-toolkit";
  @VisibleForTesting static final String projectGroup = "io.github.pixee";
  @VisibleForTesting static final String projectVersion = "0.0.2";
  private static final String nl = System.getProperty("line.separator");
  private static final Logger LOG = LoggerFactory.getLogger(DependencyInjectingVisitor.class);
}
