package io.codemodder;

import com.contrastsecurity.sarif.Result;
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

    public SarifPluginFixOnlyCodeChanger(RuleSarif sarif, Class<? extends Node> nodeType, SourceCodeRegionExtractor<Result> regionExtractor, RegionNodeMatcher regionNodeMatcher) {
        super(sarif, nodeType, regionExtractor, regionNodeMatcher);
    }

    public SarifPluginFixOnlyCodeChanger(RuleSarif sarif, Class<? extends Node> nodeType, SourceCodeRegionExtractor<Result> regionExtractor, RegionNodeMatcher regionNodeMatcher, CodemodReporterStrategy reporter) {
        super(sarif, nodeType, regionExtractor, regionNodeMatcher, reporter);
    }

    public SarifPluginFixOnlyCodeChanger(RuleSarif sarif, Class<? extends Node> nodeType) {
        super(sarif, nodeType);
    }

    public SarifPluginFixOnlyCodeChanger(RuleSarif sarif, Class<? extends Node> nodeType, SourceCodeRegionExtractor<Result> regionExtractor) {
        super(sarif, nodeType, regionExtractor);
    }

    public SarifPluginFixOnlyCodeChanger(RuleSarif sarif, Class<? extends Node> nodeType, CodemodReporterStrategy codemodReporterStrategy) {
        super(sarif, nodeType, codemodReporterStrategy);
    }

    public SarifPluginFixOnlyCodeChanger(RuleSarif sarif, Class<? extends Node> nodeType, RegionNodeMatcher regionNodeMatcher) {
        super(sarif, nodeType, regionNodeMatcher);
    }

    @Override
  public Optional<FixedFinding> getFixedFinding(String id) {
    return Optional.of(new FixedFinding(id, getDetectorRule()));
  }
}
