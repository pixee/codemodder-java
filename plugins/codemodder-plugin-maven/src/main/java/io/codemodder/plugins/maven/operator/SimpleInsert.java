package io.codemodder.plugins.maven.operator;

import java.util.List;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 * Represents a POM Upgrade Strategy by simply adding a dependency/ section (and optionally a
 * dependencyManagement/ section as well)
 */
public class SimpleInsert implements Command {
  @Override
  public boolean execute(ProjectModel pm) {
    List<Node> dependencyManagementNodeList =
        Util.selectXPathNodes(pm.getPomFile().getResultPom(), "/m:project/m:dependencyManagement");

    Element dependenciesNode;
    if (dependencyManagementNodeList.isEmpty()) {
      Element newDependencyManagementNode =
          Util.addIndentedElement(
              pm.getPomFile().getResultPom().getRootElement(),
              pm.getPomFile(),
              "dependencyManagement");

      dependenciesNode =
          Util.addIndentedElement(newDependencyManagementNode, pm.getPomFile(), "dependencies");
    } else {
      dependenciesNode = ((Element) dependencyManagementNodeList.get(0)).element("dependencies");
    }

    Element dependencyNode = appendCoordinates(dependenciesNode, pm);

    Element versionNode = Util.addIndentedElement(dependencyNode, pm.getPomFile(), "version");

    Util.upgradeVersionNode(pm, versionNode, pm.getPomFile());

    List<Node> dependenciesNodeList =
        Util.selectXPathNodes(pm.getPomFile().getResultPom(), "//m:project/m:dependencies");

    Element rootDependencyNode;
    if (dependenciesNodeList.isEmpty()) {
      rootDependencyNode =
          Util.addIndentedElement(
              pm.getPomFile().getResultPom().getRootElement(), pm.getPomFile(), "dependencies");
    } else if (dependenciesNodeList.size() == 1) {
      rootDependencyNode = (Element) dependenciesNodeList.get(0);
    } else {
      throw new IllegalStateException("More than one dependencies node");
    }

    appendCoordinates(rootDependencyNode, pm);

    return true;
  }

  /** Creates the XML Elements for a given dependency */
  private Element appendCoordinates(Element dependenciesNode, ProjectModel c) {
    Element dependencyNode =
        Util.addIndentedElement(dependenciesNode, c.getPomFile(), "dependency");

    Element groupIdNode = Util.addIndentedElement(dependencyNode, c.getPomFile(), "groupId");

    Dependency dep = c.getDependency();
    if (dep != null) {
      groupIdNode.setText(dep.getGroupId());
    }

    Element artifactIdNode = Util.addIndentedElement(dependencyNode, c.getPomFile(), "artifactId");

    if (dep != null) {
      artifactIdNode.setText(dep.getArtifactId());
    }

    return dependencyNode;
  }

  @Override
  public boolean postProcess(ProjectModel c) {
    return false;
  }
}
