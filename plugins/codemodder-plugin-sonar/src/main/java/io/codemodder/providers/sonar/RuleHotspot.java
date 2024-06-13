package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.Hotspot;
import java.nio.file.Path;
import java.util.List;

/** Class type to bind {@link Hotspot} from {@link ProvidedSonarScan} */
public final class RuleHotspot extends DefaultRuleFinding<Hotspot> {
  RuleHotspot(List<Hotspot> hotspots, final Path repository) {
    super(hotspots, repository);
  }
}
