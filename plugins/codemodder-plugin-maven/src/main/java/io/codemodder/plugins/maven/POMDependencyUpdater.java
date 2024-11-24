package io.codemodder.plugins.maven;

import io.codemodder.DependencyGAV;
import io.codemodder.DependencyUpdateResult;
import java.nio.file.Path;
import java.util.List;

interface POMDependencyUpdater {

  DependencyUpdateResult execute(Path projectDir, Path file, List<DependencyGAV> dependencies);
}
