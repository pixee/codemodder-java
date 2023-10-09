package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.dom4j.Element;
import org.dom4j.Node;

public abstract class AbstractCommand implements Command {

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

  protected File getLocalRepositoryPath(ProjectModel pm) {
    File localRepositoryPath = null;

    if (pm.getRepositoryPath() != null) {
      localRepositoryPath = pm.getRepositoryPath();
    } else if (System.getenv("M2_REPO") != null) {
      localRepositoryPath = new File(System.getenv("M2_REPO"));
    } else if (System.getProperty("maven.repo.local") != null) {
      localRepositoryPath = new File(System.getProperty("maven.repo.local"));
    } else {
      localRepositoryPath = new File(System.getProperty("user.home"), ".m2/repository");
    }

    return localRepositoryPath;
  }
}
