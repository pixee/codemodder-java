package io.codemodder.providers.sonar;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import io.codemodder.CodemodInvocationContext;
import io.codemodder.CodemodReporterStrategy;
import io.codemodder.NodeCollector;
import io.codemodder.RegionNodeMatcher;
import io.codemodder.javaparser.ChangesResult;
import io.codemodder.sonar.model.Hotspot;
import io.codemodder.sonar.model.SonarFinding;

/** Provides base functionality for making JavaParser-based changes based on Sonar hotspots. */
public abstract class SonarHotspotsPluginJavaParserChanger<T extends Node>
    extends SonarPluginJavaParserChanger<T> {

  protected SonarHotspotsPluginJavaParserChanger(
      RuleHotspot ruleHotspot,
      Class<? extends Node> nodeType,
      RegionNodeMatcher regionNodeMatcher,
      NodeCollector nodeCollector) {
    super(ruleHotspot, nodeType, regionNodeMatcher, nodeCollector);
  }

  protected SonarHotspotsPluginJavaParserChanger(
      RuleHotspot ruleHotspot, Class<? extends Node> nodeType) {
    super(ruleHotspot, nodeType);
  }

  protected SonarHotspotsPluginJavaParserChanger(
      RuleHotspot ruleHotspot,
      Class<? extends Node> nodeType,
      RegionNodeMatcher regionNodeMatcher,
      CodemodReporterStrategy codemodReporterStrategy) {
    super(ruleHotspot, nodeType, regionNodeMatcher, codemodReporterStrategy);
  }

  @Override
  protected ChangesResult onFindingFound(
      final CodemodInvocationContext context,
      final CompilationUnit cu,
      final T node,
      final SonarFinding sonarFinding) {
    return onHotspotFound(context, cu, node, (Hotspot) sonarFinding);
  }

  public abstract ChangesResult onHotspotFound(
      CodemodInvocationContext context, CompilationUnit cu, T node, Hotspot hotspot);
}
