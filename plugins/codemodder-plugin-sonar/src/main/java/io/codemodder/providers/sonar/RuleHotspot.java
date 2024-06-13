package io.codemodder.providers.sonar;

import io.codemodder.sonar.model.Hotspot;
import java.nio.file.Path;
import java.util.List;

public class RuleHotspot extends DefaultRuleFinding<Hotspot> {
  RuleHotspot(List<Hotspot> sonarFindings, final Path repository) {
    super(sonarFindings, repository);
  }
}
