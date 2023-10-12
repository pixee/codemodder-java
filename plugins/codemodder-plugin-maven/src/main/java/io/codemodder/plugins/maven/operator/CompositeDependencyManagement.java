package io.codemodder.plugins.maven.operator;

import java.util.List;
import org.dom4j.Element;
import org.dom4j.Node;

/**
 * A command for managing dependencies within a composite POM, including dependency management and
 * addition.
 */
class CompositeDependencyManagement extends AbstractCommand {

  /**
   * Executes the CompositeDependencyManagement command to manage dependencies in a composite POM.
   *
   * @param pm ProjectModel containing project information.
   * @return true if the command modifies the POM and sets it as dirty, false otherwise.
   */
  @Override
  public boolean execute(ProjectModel pm) {
    // Abort if not multi-pom
    if (pm.getParentPomFiles().isEmpty()) {
      return false;
    }

    boolean result = false;

    // TODO: Make it configurable / clear WHERE one should change it
    POMDocument parentPomFile = pm.getParentPomFiles().get(pm.getParentPomFiles().size() - 1);

    // add dependencyManagement
    Element dependencyManagementElement;
    if (parentPomFile.getResultPom().getRootElement().elements("dependencyManagement").isEmpty()) {
      dependencyManagementElement =
          Util.addIndentedElement(
              parentPomFile.getResultPom().getRootElement(), parentPomFile, "dependencyManagement");
    } else {
      dependencyManagementElement =
          parentPomFile.getResultPom().getRootElement().element("dependencyManagement");
    }

    Element newDependencyManagementElement =
        modifyDependency(
            parentPomFile,
            Util.buildLookupExpressionForDependencyManagement(pm.getDependency()),
            pm,
            dependencyManagementElement,
            true);

    if (pm.isUseProperties()) {
      if (newDependencyManagementElement != null) {
        Element newVersionNode =
            Util.addIndentedElement(newDependencyManagementElement, parentPomFile, "version");
        Util.upgradeVersionNode(pm, newVersionNode, parentPomFile);
      } else {
        throw new IllegalStateException("newDependencyManagementElement is missing");
      }
    }

    // add dependency to pom - sans version
    modifyDependency(
        pm.getPomFile(),
        Util.buildLookupExpressionForDependency(pm.getDependency()),
        pm,
        pm.getPomFile().getResultPom().getRootElement(),
        false);

    if (!result) {
      result = pm.getPomFile().getDirty();
    }

    return result;
  }

  private Element modifyDependency(
      POMDocument pomFileToModify,
      String lookupExpressionForDependency,
      ProjectModel c,
      Element parentElement,
      boolean dependencyManagementNode) {
    List<Node> dependencyNodes =
        Util.selectXPathNodes(pomFileToModify.getResultPom(), lookupExpressionForDependency);

    if (dependencyNodes.size() == 1) {
      List<Node> versionNodes = Util.selectXPathNodes(dependencyNodes.get(0), "./m:version");

      if (versionNodes.size() == 1) {
        Element versionNode = (Element) versionNodes.get(0);
        versionNode.getParent().content().remove(versionNode);
        pomFileToModify.setDirty(true);
      }

      return (Element) dependencyNodes.get(0);
    } else {
      Element dependenciesNode;
      if (parentElement.element("dependencies") != null) {
        dependenciesNode = parentElement.element("dependencies");
      } else {
        dependenciesNode = Util.addIndentedElement(parentElement, pomFileToModify, "dependencies");
      }

      Element dependencyNode =
          Util.addIndentedElement(dependenciesNode, pomFileToModify, "dependency");
      Util.addIndentedElement(dependencyNode, pomFileToModify, "groupId")
          .setText(c.getDependency().getGroupId());
      Util.addIndentedElement(dependencyNode, pomFileToModify, "artifactId")
          .setText(c.getDependency().getArtifactId());

      if (dependencyManagementNode) {
        if (!c.isUseProperties()) {
          Util.addIndentedElement(dependencyNode, pomFileToModify, "version")
              .setText(c.getDependency().getVersion());
        }
      }

      pomFileToModify.setDirty(true);

      return dependencyNode;
    }
  }
}
