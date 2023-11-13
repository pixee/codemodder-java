package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

  private static final Logger LOGGER = LoggerFactory.getLogger(POMScanner.class);

  private final File originalFile;
  private final File topLevelDirectory;

  private File lastFile;

  public POMScanner(final File originalFile, final File topLevelDirectory) {
    this.originalFile = originalFile;
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
    POMDocument pomFile = POMDocumentFactory.load(originalFile);
    List<POMDocument> parentPomFiles = new ArrayList<>();

    Queue<Element> pomFileQueue = new LinkedList<>();

      Element relativePathElement = getRelativePathElement(pomFile);


      Element parentElement = getParentElement(pomFile);

      processRelativePathElement(relativePathElement, parentElement, originalFile, topLevelDirectory, pomFileQueue);


    lastFile = originalFile;

    Set<String> prevPaths = new HashSet<>();
    POMDocument prevPOMDocument = pomFile;

      processPOMFileQueue(pomFileQueue, prevPaths, parentPomFiles, prevPOMDocument, pomFile);

      return ProjectModelFactory.loadFor(pomFile, parentPomFiles);
  }

    private void processPOMFileQueue(Queue<Element> pomFileQueue, Set<String> prevPaths, List<POMDocument> parentPomFiles, POMDocument prevPOMDocument, POMDocument pomFile) throws DocumentException, IOException, URISyntaxException {
        while (!pomFileQueue.isEmpty()) {
            Element currentRelativePathElement = pomFileQueue.poll();

            if (StringUtils.isEmpty(currentRelativePathElement.getTextTrim())) {
                break;
            }

            String relativePath = fixPomRelativePath(currentRelativePathElement.getText());

            if (!processRelativePath(relativePath, prevPaths, pomFile)) {
                break;
            }

            Path newPath = resolvePath(lastFile, relativePath);

            if (!validateNewPath(newPath, topLevelDirectory)) {
                break;
            }

            POMDocument newPomFile = POMDocumentFactory.load(newPath.toFile());

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
        return (parentElement != null) ? parentElement.element("artifactId").getText() : null;
    }


    private void processRelativePathElement(Element relativePathElement, Element parentElement, File originalFile, File topLevelDirectory, Queue<Element> pomFileQueue) {
        if (relativePathElement != null && StringUtils.isNotEmpty(relativePathElement.getTextTrim())) {
            pomFileQueue.add(relativePathElement);
        } else if (relativePathElement == null && parentElement != null && isNotRoot(originalFile, topLevelDirectory)) {
            addDefaultRelativePathElement(pomFileQueue);
        }
    }

    private boolean isNotRoot(File originalFile, File topLevelDirectory) {
        return !originalFile.getParentFile().equals(topLevelDirectory);
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

    private boolean processRelativePath(String relativePath, Set<String> prevPaths, POMDocument pomFile) {

        if (!isRelative(relativePath)) {
            LOGGER.warn("not relative: " + relativePath);
            return false;
        }

        if (prevPaths.contains(relativePath)) {
            LOGGER.warn("loop: " + pomFile.getFile() + ", relativePath: " + relativePath);
            return false;
        } else {
            prevPaths.add(relativePath);
        }

        return true;
    }

    private boolean validateNewPath(Path newPath, File topLevelDirectory) {
        if (Files.notExists(newPath)) {
            LOGGER.warn("new path does not exist: " + newPath);
            return false;
        }

        if (newPath.toFile().length() == 0) {
            LOGGER.warn("File has zero length: " + newPath);
            return false;
        }

        if (!newPath.startsWith(topLevelDirectory.getAbsolutePath())) {
            LOGGER.warn(
                    "Not a child: " + newPath + " (absolute: " + topLevelDirectory.getAbsolutePath() + ")");
            return false;
        }

        return true;
    }

    private boolean checkArtifactIdMatch(POMDocument newPomFile, POMDocument prevPOMDocument) {

        String myArtifactId =
                newPomFile.getPomDocument().getRootElement().element("artifactId") != null
                        ? newPomFile.getPomDocument().getRootElement().element("artifactId").getText()
                        : null;

        String prevParentArtifactId =
                getParentArtifactId(prevPOMDocument);

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

  private Path resolvePath(final File baseFile, final String relativePath) {
    File parentDir = baseFile;

    if (parentDir.isFile()) {
      parentDir = parentDir.getParentFile();
    }

    File result = new File(new File(parentDir, relativePath).toURI().normalize().getPath());

    lastFile = result.isDirectory() ? result : result.getParentFile();

    return Paths.get(result.getAbsolutePath());
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
