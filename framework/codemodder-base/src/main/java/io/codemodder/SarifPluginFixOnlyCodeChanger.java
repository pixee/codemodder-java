package io.codemodder;

import com.github.javaparser.ast.Node;
import io.codemodder.codetf.FixedFinding;
import java.util.Optional;

public abstract class SarifPluginFixOnlyCodeChanger<T extends Node>
    extends SarifPluginJavaParserChanger<T> implements FixOnlyCodeChanger {

  protected SarifPluginFixOnlyCodeChanger(
      final RuleSarif sarif,
      final Class<? extends Node> nodeType,
      final RegionNodeMatcher regionNodeMatcher,
      final CodemodReporterStrategy reporterStrategy) {
    super(sarif, nodeType, regionNodeMatcher, reporterStrategy);
  }

  @Override
  public Optional<FixedFinding> getFixedFinding(String id) {
    return Optional.of(new FixedFinding(id, getDetectorRule()));
  }
}
