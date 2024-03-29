package io.codemodder.codemods;

import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import io.codemodder.ProjectProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

/**
 * This class provides static inner classes implementing {@link ProjectProvider}, each returning
 * different dependencies. These dependencies are used for testing the behavior of {@link
 * io.codemodder.codemods.HardenXStreamCodemod} under various XStream dependency versions.
 */
final class HardenXStreamCodemodTestProjectProvider {

  public static class XStreamSameDependencyVersion implements ProjectProvider {
    @Override
    public DependencyUpdateResult updateDependencies(
        Path projectDir, Path file, List<DependencyGAV> remainingFileDependencies)
        throws IOException {
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    @Override
    public Collection<DependencyGAV> getAllDependencies(Path projectDir, Path file) {
      return List.of(DependencyGAV.createDefault("com.thoughtworks.xstream", "xstream", "1.4.8"));
    }
  }

  public static class XStreamLowerDependencyVersion implements ProjectProvider {
    @Override
    public DependencyUpdateResult updateDependencies(
        Path projectDir, Path file, List<DependencyGAV> remainingFileDependencies)
        throws IOException {
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    @Override
    public Collection<DependencyGAV> getAllDependencies(Path projectDir, Path file) {
      return List.of(DependencyGAV.createDefault("com.thoughtworks.xstream", "xstream", "1.4.7"));
    }
  }

  public static class XStreamGreaterDependencyVersion implements ProjectProvider {
    @Override
    public DependencyUpdateResult updateDependencies(
        Path projectDir, Path file, List<DependencyGAV> remainingFileDependencies)
        throws IOException {
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    @Override
    public Collection<DependencyGAV> getAllDependencies(Path projectDir, Path file) {
      return List.of(DependencyGAV.createDefault("com.thoughtworks.xstream", "xstream", "1.4.9"));
    }
  }

  public static class XStreamUnparseableDependencyVersion implements ProjectProvider {
    @Override
    public DependencyUpdateResult updateDependencies(
        Path projectDir, Path file, List<DependencyGAV> remainingFileDependencies)
        throws IOException {
      return DependencyUpdateResult.EMPTY_UPDATE;
    }

    @Override
    public Collection<DependencyGAV> getAllDependencies(Path projectDir, Path file) {
      return List.of(DependencyGAV.createDefault("com.thoughtworks.xstream", "xstream", ""));
    }
  }
}
