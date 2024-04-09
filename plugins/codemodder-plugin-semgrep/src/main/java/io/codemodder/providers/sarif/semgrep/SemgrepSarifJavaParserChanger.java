package io.codemodder.providers.sarif.semgrep;

import com.contrastsecurity.sarif.Result;
import com.github.javaparser.ast.Node;
import io.codemodder.CodemodReporterStrategy;
import io.codemodder.FixOnlyCodeChanger;
import io.codemodder.RegionNodeMatcher;
import io.codemodder.RuleSarif;
import io.codemodder.SarifPluginJavaParserChanger;
import io.codemodder.SourceCodeRegionExtractor;

public abstract class SemgrepSarifJavaParserChanger<T extends Node>
    extends SarifPluginJavaParserChanger<T> implements FixOnlyCodeChanger {

  protected SemgrepSarifJavaParserChanger(RuleSarif sarif, Class<? extends Node> nodeType) {
    super(sarif, nodeType);
  }

  protected SemgrepSarifJavaParserChanger(
      RuleSarif sarif,
      Class<? extends Node> nodeType,
      SourceCodeRegionExtractor<Result> regionExtractor) {
    super(sarif, nodeType, regionExtractor);
  }

  protected SemgrepSarifJavaParserChanger(
      RuleSarif sarif,
      Class<? extends Node> nodeType,
      CodemodReporterStrategy codemodReporterStrategy) {
    super(sarif, nodeType, codemodReporterStrategy);
  }

  protected SemgrepSarifJavaParserChanger(
      RuleSarif sarif, Class<? extends Node> nodeType, RegionNodeMatcher regionNodeMatcher) {
    super(sarif, nodeType, regionNodeMatcher);
  }

  protected SemgrepSarifJavaParserChanger(
      RuleSarif sarif,
      Class<? extends Node> nodeType,
      RegionNodeMatcher regionNodeMatcher,
      CodemodReporterStrategy reporterStrategy) {
    super(sarif, nodeType, regionNodeMatcher, reporterStrategy);
  }

  protected SemgrepSarifJavaParserChanger(
      RuleSarif sarif,
      Class<? extends Node> nodeType,
      SourceCodeRegionExtractor<Result> regionExtractor,
      RegionNodeMatcher regionNodeMatcher) {
    super(sarif, nodeType, regionExtractor, regionNodeMatcher);
  }

  protected SemgrepSarifJavaParserChanger(
      RuleSarif sarif,
      Class<? extends Node> nodeType,
      SourceCodeRegionExtractor<Result> regionExtractor,
      RegionNodeMatcher regionNodeMatcher,
      CodemodReporterStrategy reporter) {
    super(sarif, nodeType, regionExtractor, regionNodeMatcher, reporter);
  }

  @Override
  public String vendorName() {
    return "Semgrep";
  }
}
