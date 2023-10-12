package io.codemodder.plugins.maven.operator;

import java.util.Collection;
import java.util.List;
import org.dom4j.Element;
import org.dom4j.Text;

/** Guard Command Singleton used to validate required parameters */
class CheckParentPackaging extends AbstractCommand {
  private static final CheckParentPackaging INSTANCE = new CheckParentPackaging();

  private CheckParentPackaging() {}

  /**
   * Gets the singleton instance of the CheckParentPackaging command.
   *
   * @return The singleton instance of CheckParentPackaging.
   */
  public static CheckParentPackaging getInstance() {
    return INSTANCE;
  }

  private boolean packagingTypePredicate(POMDocument d, String packagingType) {
    List<?> elementTextList =
        Util.selectXPathNodes(d.getPomDocument().getRootElement(), "/m:project/m:packaging/text()");
    Object elementText = elementTextList.isEmpty() ? null : elementTextList.get(0);

    if (elementText instanceof Text) {
      return ((Text) elementText).getText().equals(packagingType);
    }

    return false;
  }

  /**
   * Checks and validates the packaging type for parent POMs and the main POM file.
   *
   * @param pm ProjectModel containing project information.
   * @return false, indicating that the packaging type checks were performed without errors.
   * @throws WrongDependencyTypeException if the packaging type is incorrect for parent POMs or the
   *     main POM file.
   */
  @Override
  public boolean execute(ProjectModel pm) {
    Collection<POMDocument> wrongParentPoms =
        pm.getParentPomFiles().stream()
            .filter(pomFile -> !packagingTypePredicate(pomFile, "pom"))
            .toList();

    if (!wrongParentPoms.isEmpty()) {
      throw new WrongDependencyTypeException("Wrong packaging type for parentPom");
    }

    if (!pm.getParentPomFiles().isEmpty()) {
      // check main pom file has a inheritance to one of the members listed
      if (!hasValidParentAndPackaging(pm.getPomFile())) {
        throw new WrongDependencyTypeException("Invalid parent/packaging combo for main pomfile");
      }
    }

    // TODO: Test a->b->c

    return false;
  }

  private boolean hasValidParentAndPackaging(POMDocument pomFile) {
    List<?> parentNodes =
        Util.selectXPathNodes(pomFile.getPomDocument().getRootElement(), "/m:project/m:parent");
    Element parentNode = parentNodes.isEmpty() ? null : (Element) parentNodes.get(0);

    if (parentNode == null) {
      return false;
    }

    List<?> packagingNodes =
        Util.selectXPathNodes(
            pomFile.getPomDocument().getRootElement(), "/m:project/m:packaging/text()");
    String packagingText =
        packagingNodes.isEmpty() ? "jar" : ((Text) packagingNodes.get(0)).getText();

    boolean validPackagingType = packagingText.endsWith("ar");

    return validPackagingType;
  }
}
