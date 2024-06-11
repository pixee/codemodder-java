package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.Hotspot;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class DefaultRuleHotspots implements RuleHotspots {

  private final Map<String, List<Hotspot>> hotspots;

  DefaultRuleHotspots(final Map<String, List<Hotspot>> hotspots) {
    this.hotspots = Objects.requireNonNull(hotspots);
  }

  @Override
  public List<Hotspot> getResultsByPath(final Path path) {
    return hotspots.get(path.toString());
  }

  @Override
  public boolean hasResults() {
    return !hotspots.isEmpty();
  }
}
