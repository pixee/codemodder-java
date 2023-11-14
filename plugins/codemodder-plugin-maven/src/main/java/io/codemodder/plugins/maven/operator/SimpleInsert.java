package io.codemodder.plugins.maven.operator;

import java.util.List;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 * Represents a POM Upgrade Strategy by simply adding a dependency/ section (and optionally a
 * dependencyManagement/ section as well)
 */
class SimpleInsert implements Command {

  private final boolean validateDependencyExistence;

  /**
   * @param validateDependencyExistence is true when we only want to this command alone to insert
   *     dependencies in a CommandChain for dependency existence validations
   */
  SimpleInsert(final boolean validateDependencyExistence) {
    this.validateDependencyExistence = validateDependencyExistence;
  }

  /**
   * Executes the POM upgrade strategy by adding a dependency and optionally a dependencyManagement
   * section.
   *
   * @param pm The ProjectModel containing the configuration and settings for the upgrade.
   * @return `true` if the upgrade is successful, `false` otherwise.
   */
  @Override
  public boolean execute(ProjectModel pm) {

    if (validateDependencyExistence && checkDependencyExists(pm)) {
      return true;
    }

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

  private boolean checkDependencyExists(final ProjectModel pm) {
    String lookupExpressionForDependency =
        Util.buildLookupExpressionForDependency(pm.getDependency());

    List<Node> matchedDependencies =
        Util.selectXPathNodes(pm.getPomFile().getResultPom(), lookupExpressionForDependency);

    return !matchedDependencies.isEmpty();
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

  /**
   * This method is not used for this strategy and always returns `false`.
   *
   * @param c The ProjectModel.
   * @return `false`.
   */
  @Override
  public boolean postProcess(ProjectModel c) {
    return false;
  }
}
