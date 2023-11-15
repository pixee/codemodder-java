package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.tree.DefaultElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The POMScanner class provides methods for scanning Maven POM (Project Object Model) files,
 * including the original POM and its parent POMs, to create a ProjectModelFactory. This class
 * offers both modern and legacy scanning methods for flexibility.
 */
class POMScanner {

  private static final Pattern RE_WINDOWS_PATH = Pattern.compile("^[A-Za-z]:");

  private static final String ARTIFACT_ID = "artifactId";

  private static final Logger LOGGER = LoggerFactory.getLogger(POMScanner.class);

  private final Path originalPath;
  private final Path topLevelDirectory;

  private Path lastPath;

  public POMScanner(final Path originalPath, final Path topLevelDirectory) {
    this.originalPath = originalPath;
    this.topLevelDirectory = topLevelDirectory;
  }

  /**
   * Scans a POM file and its parent POMs using the legacy method and creates a ProjectModelFactory.
   *
   * @return A ProjectModelFactory representing the scanned POMs.
   * @throws DocumentException If a document error occurs.
   * @throws IOException If an I/O error occurs.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   */
  public ProjectModelFactory scanFrom() throws DocumentException, IOException, URISyntaxException {
    POMDocument pomFile = POMDocumentFactory.load(originalPath);
    List<POMDocument> parentPomFiles = new ArrayList<>();

    Queue<Element> pomFileQueue = new LinkedList<>();

    Element relativePathElement = getRelativePathElement(pomFile);

    Element parentElement = getParentElement(pomFile);

    processRelativePathElement(
        relativePathElement, parentElement, originalPath, topLevelDirectory, pomFileQueue);

    lastPath = originalPath;

    Set<String> prevPaths = new HashSet<>();
    POMDocument prevPOMDocument = pomFile;

    processPOMFileQueue(pomFileQueue, prevPaths, parentPomFiles, prevPOMDocument, pomFile);

    return ProjectModelFactory.loadFor(pomFile, parentPomFiles);
  }

  private void processPOMFileQueue(
      Queue<Element> pomFileQueue,
      Set<String> prevPaths,
      List<POMDocument> parentPomFiles,
      POMDocument prevPOMDocument,
      POMDocument pomFile)
      throws DocumentException, IOException, URISyntaxException {
    while (!pomFileQueue.isEmpty()) {
      Element currentRelativePathElement = pomFileQueue.poll();

      if (StringUtils.isEmpty(currentRelativePathElement.getTextTrim())) {
        break;
      }

      String relativePath = fixPomRelativePath(currentRelativePathElement.getText());

      if (!processRelativePath(relativePath, prevPaths, pomFile)) {
        break;
      }

      Path newPath = resolvePath(lastPath, relativePath);

      if (!validateNewPath(newPath, topLevelDirectory)) {
        break;
      }

      POMDocument newPomFile = POMDocumentFactory.load(newPath);

      processParentAndRelativePathElements(newPomFile);

      if (!checkArtifactIdMatch(newPomFile, prevPOMDocument)) {
        break;
      }

      parentPomFiles.add(newPomFile);
      prevPOMDocument = newPomFile;

      Element newRelativePathElement = getRelativePathElement(newPomFile);

      if (newRelativePathElement != null) {
        pomFileQueue.add(newRelativePathElement);
      }
    }
  }

  private Element getParentElement(POMDocument pomFile) {
    return pomFile.getPomDocument().getRootElement().element("parent");
  }

  private Element getRelativePathElement(POMDocument pomFile) {
    Element parentElement = getParentElement(pomFile);
    return (parentElement != null) ? parentElement.element("relativePath") : null;
  }

  private String getParentArtifactId(POMDocument prevPOMDocument) {
    Element parentElement = getParentElement(prevPOMDocument);
    return (parentElement != null) ? parentElement.element(ARTIFACT_ID).getText() : null;
  }

  private void processRelativePathElement(
      Element relativePathElement,
      Element parentElement,
      Path originalPath,
      Path topLevelDirectory,
      Queue<Element> pomFileQueue) {
    if (relativePathElement != null && StringUtils.isNotEmpty(relativePathElement.getTextTrim())) {
      pomFileQueue.add(relativePathElement);
    } else if (relativePathElement == null
        && parentElement != null
        && isNotRoot(originalPath, topLevelDirectory)) {
      addDefaultRelativePathElement(pomFileQueue);
    }
  }

  private boolean isNotRoot(Path originalPath, Path topLevelDirectory) {
    return !originalPath.getParent().equals(topLevelDirectory);
  }

  private DefaultElement createRelativePathElement() {
    DefaultElement newRelativePathElement = new DefaultElement("relativePath");
    newRelativePathElement.setText("../pom.xml");
    return newRelativePathElement;
  }

  private void addDefaultRelativePathElement(Queue<Element> pomFileQueue) {
    DefaultElement newRelativePathElement = createRelativePathElement();
    pomFileQueue.add(newRelativePathElement);
  }

  private void processParentAndRelativePathElements(POMDocument newPomFile) {
    boolean hasParent = getParentElement(newPomFile) != null;
    boolean hasRelativePath = getRelativePathElement(newPomFile) != null;

    if (!hasRelativePath && hasParent) {
      Element parentElement = newPomFile.getPomDocument().getRootElement().element("parent");
      DefaultElement newRelativePathElement = createRelativePathElement();
      parentElement.add(newRelativePathElement);
    }
  }

  private boolean processRelativePath(
      String relativePath, Set<String> prevPaths, POMDocument pomFile) {

    if (!isRelative(relativePath)) {
      LOGGER.warn("not relative: " + relativePath);
      return false;
    }

    if (prevPaths.contains(relativePath)) {
      LOGGER.warn("loop: " + pomFile.getPath() + ", relativePath: " + relativePath);
      return false;
    } else {
      prevPaths.add(relativePath);
    }

    return true;
  }

  private boolean validateNewPath(Path newPath, Path topLevelDirectory) {
    try {
      if (Files.notExists(newPath)) {
        LOGGER.warn("new path does not exist: " + newPath);
        return false;
      }

      if (Files.size(newPath) == 0) {
        LOGGER.warn("File has zero length: " + newPath);
        return false;
      }

      if (!newPath.startsWith(topLevelDirectory)) {
        LOGGER.warn(
            "Not a child: " + newPath + " (absolute: " + topLevelDirectory.toAbsolutePath() + ")");
        return false;
      }

      return true;
    } catch (IOException e) {
      LOGGER.error("Error while validating path: " + newPath, e);
      return false;
    }
  }

  private boolean checkArtifactIdMatch(POMDocument newPomFile, POMDocument prevPOMDocument) {

    String myArtifactId =
        newPomFile.getPomDocument().getRootElement().element(ARTIFACT_ID) != null
            ? newPomFile.getPomDocument().getRootElement().element(ARTIFACT_ID).getText()
            : null;

    String prevParentArtifactId = getParentArtifactId(prevPOMDocument);

    if (myArtifactId == null || prevParentArtifactId == null) {
      LOGGER.warn(
          "Missing previous mine or parent: " + myArtifactId + " / " + prevParentArtifactId);
      return false;
    }

    if (!myArtifactId.equals(prevParentArtifactId)) {
      LOGGER.warn("Previous doesn't match: " + myArtifactId + " / " + prevParentArtifactId);
      return false;
    }

    return true;
  }

  private Path resolvePath(final Path baseFile, final String relativePath) {
    Path parentDir = baseFile;

    if (parentDir != null && !Files.isDirectory(parentDir)) {
      parentDir = parentDir.getParent();
    }

    assert parentDir != null;
    Path result = parentDir.resolve(relativePath).normalize().toAbsolutePath();

    lastPath = Files.isDirectory(result) ? result : result.getParent();

    return result;
  }

  private String fixPomRelativePath(final String text) {
    if (text == null) {
      return "";
    }

    String name = new File(text).getName();

    if (name.indexOf('.') == -1) {
      return text + "/pom.xml";
    }

    return text;
  }

  private boolean isRelative(final String path) {
    if (RE_WINDOWS_PATH.matcher(path).matches()) {
      return false;
    }

    return !(path.startsWith("/") || path.startsWith("~"));
  }
}
