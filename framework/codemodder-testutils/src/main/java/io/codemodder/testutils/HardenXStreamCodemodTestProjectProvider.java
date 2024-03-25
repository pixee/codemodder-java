package io.codemodder.testutils;

import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import io.codemodder.ProjectProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class HardenXStreamCodemodTestProjectProvider implements ProjectProvider {

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
