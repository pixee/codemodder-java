package io.codemodder.plugins.maven.operator;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.dom4j.Element;
import org.dom4j.Node;

/** Base implementation of Command - used by SimpleDependency and SimpleInsert */
abstract class AbstractCommand implements Command {

  /**
   * Given a POM, locate its coordinates for a given dependency based on lookupExpression and
   * determines whether an upgrade is necessary.
   *
   * @param pm The ProjectModel containing the POM and other project information.
   * @param lookupExpression An XPath expression to locate the desired dependency node.
   * @return true if an upgrade was performed, false otherwise.
   */
  protected boolean handleDependency(ProjectModel pm, String lookupExpression) {
    List<Node> dependencyNodes =
        Util.selectXPathNodes(pm.getPomFile().getResultPom(), lookupExpression);

    if (1 == dependencyNodes.size()) {
      List<Node> versionNodes = Util.selectXPathNodes(dependencyNodes.get(0), "./m:version");

      if (1 == versionNodes.size()) {
        Element versionNode = (Element) versionNodes.get(0);

        boolean mustUpgrade = true;

        if (pm.isSkipIfNewer()) {
          mustUpgrade = Util.findOutIfUpgradeIsNeeded(pm, versionNode);
        }

        if (mustUpgrade) {
          Util.upgradeVersionNode(pm, versionNode, pm.getPomFile());
        }

        return true;
      }
    }

    return false;
  }

  @Override
  public boolean execute(ProjectModel pm)
      throws URISyntaxException, IOException, XMLStreamException {
    return false;
  }

  @Override
  public boolean postProcess(ProjectModel c) throws XMLStreamException {
    return false;
  }

  /**
   * Retrieves the local repository path from various sources or default values.
   *
   * @param pm The ProjectModel containing information about the project.
   * @return A File object representing the local repository path.
   */
  protected Path getLocalRepositoryPath(ProjectModel pm) {
    Path localRepositoryPath = null;

    if (pm.getRepositoryPath() != null) {
      localRepositoryPath = pm.getRepositoryPath();
    } else if (System.getenv("M2_REPO") != null) {
      localRepositoryPath = Paths.get(System.getenv("M2_REPO"));
    } else if (System.getProperty("maven.repo.local") != null) {
      localRepositoryPath = Paths.get(System.getProperty("maven.repo.local"));
    } else {
      localRepositoryPath = Paths.get(System.getProperty("user.home"), ".m2/repository");
    }

    return localRepositoryPath;
  }
}
