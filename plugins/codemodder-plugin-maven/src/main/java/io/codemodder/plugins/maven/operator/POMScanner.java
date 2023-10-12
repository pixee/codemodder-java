package io.codemodder.plugins.maven.operator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelBuildingResult;
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
public class POMScanner {

  private static final Pattern RE_WINDOWS_PATH = Pattern.compile("^[A-Za-z]:");

  private static final Logger LOGGER = LoggerFactory.getLogger(POMScanner.class);

  /**
   * Scans a POM file and its parent POMs, if any, and creates a ProjectModelFactory.
   *
   * @param originalFile The original POM file to scan.
   * @param topLevelDirectory The top-level directory containing the POM files.
   * @return A ProjectModelFactory representing the scanned POMs.
   * @throws Exception If an error occurs during the scanning process.
   */
  public static ProjectModelFactory scanFrom(File originalFile, File topLevelDirectory)
      throws Exception {
    ProjectModelFactory originalDocument = ProjectModelFactory.load(originalFile);

    List<File> parentPoms;
    try {
      parentPoms = getParentPoms(originalFile);
    } catch (Exception e) {
      if (e instanceof ModelBuildingException) {
        Ignorable.LOGGER.debug("mbe (you can ignore): ", e);
      } else {
        LOGGER.warn("While trying embedder: ", e);
      }
      return legacyScanFrom(originalFile, topLevelDirectory);
    }

    try {
      List<POMDocument> parentPomDocuments =
          parentPoms.stream()
              .map(
                  file -> {
                    try {
                      return POMDocumentFactory.load(file);
                    } catch (IOException | URISyntaxException | DocumentException e) {

                      return null;
                    }
                  })
              .filter(Objects::nonNull)
              .collect(Collectors.toList());

      return originalDocument.withParentPomFiles(parentPomDocuments);
    } catch (Exception e) {

      return originalDocument;
    }
  }

  /**
   * Scans a POM file and its parent POMs using the legacy method and creates a ProjectModelFactory.
   *
   * @param originalFile The original POM file to scan.
   * @param topLevelDirectory The top-level directory containing the POM files.
   * @return A ProjectModelFactory representing the scanned POMs.
   * @throws DocumentException If a document error occurs.
   * @throws IOException If an I/O error occurs.
   * @throws URISyntaxException If there is an issue with the URI syntax.
   */
  public static ProjectModelFactory legacyScanFrom(File originalFile, File topLevelDirectory)
      throws DocumentException, IOException, URISyntaxException {
    POMDocument pomFile = POMDocumentFactory.load(originalFile);
    List<POMDocument> parentPomFiles = new ArrayList<>();

    Queue<Element> pomFileQueue = new LinkedList<>();

    Element relativePathElement =
        pomFile.getPomDocument().getRootElement().element("parent") != null
            ? pomFile.getPomDocument().getRootElement().element("parent").element("relativePath")
            : null;

    Element parentElement = pomFile.getPomDocument().getRootElement().element("parent");

    if (relativePathElement != null && StringUtils.isNotEmpty(relativePathElement.getTextTrim())) {
      pomFileQueue.add(relativePathElement);
    } else if (relativePathElement == null && parentElement != null) {
      // Skip trying to find a parent if we are at the root
      if (!originalFile.getParentFile().equals(topLevelDirectory)) {
        DefaultElement newRelativePathElement = new DefaultElement("relativePath");
        newRelativePathElement.setText("../pom.xml");
        pomFileQueue.add(newRelativePathElement);
      }
    }

    Set<String> prevPaths = new HashSet<>();
    POMDocument prevPOMDocument = pomFile;

    while (!pomFileQueue.isEmpty()) {
      Element currentRelativePathElement = pomFileQueue.poll();

      if (StringUtils.isEmpty(currentRelativePathElement.getTextTrim())) {
        break;
      }

      String relativePath = fixPomRelativePath(currentRelativePathElement.getText());

      if (!isRelative(relativePath)) {
        LOGGER.warn("not relative: " + relativePath);
        break;
      }

      if (prevPaths.contains(relativePath)) {
        LOGGER.warn("loop: " + pomFile.getFile() + ", relativePath: " + relativePath);
        break;
      } else {
        prevPaths.add(relativePath);
      }

      Path newPath = POMScanner.resolvePath(originalFile, relativePath);

      if (!newPath.toFile().exists()) {
        LOGGER.warn("new path does not exist: " + newPath);
        break;
      }

      if (newPath.toFile().length() == 0) {
        LOGGER.warn("File has zero length: " + newPath);
        break;
      }

      if (!newPath.startsWith(topLevelDirectory.getAbsolutePath())) {
        LOGGER.warn(
            "Not a child: " + newPath + " (absolute: " + topLevelDirectory.getAbsolutePath() + ")");
        break;
      }

      POMDocument newPomFile = POMDocumentFactory.load(newPath.toFile());

      boolean hasParent = newPomFile.getPomDocument().getRootElement().element("parent") != null;
      boolean hasRelativePath =
          newPomFile.getPomDocument().getRootElement().element("parent") != null
              && newPomFile
                      .getPomDocument()
                      .getRootElement()
                      .element("parent")
                      .element("relativePath")
                  != null;

      if (!hasRelativePath && hasParent) {
        Element parentElement2 = newPomFile.getPomDocument().getRootElement().element("parent");
        DefaultElement newRelativePathElement = new DefaultElement("relativePath");
        newRelativePathElement.setText("../pom.xml");
        parentElement2.add(newRelativePathElement);
      }

      // One Last Test - Does the previous mention at least ArtifactId equals to parent declared at
      // previous?
      // If not break and warn

      String myArtifactId =
          newPomFile.getPomDocument().getRootElement().element("artifactId") != null
              ? newPomFile.getPomDocument().getRootElement().element("artifactId").getText()
              : null;

      String prevParentArtifactId =
          prevPOMDocument.getPomDocument().getRootElement().element("parent") != null
              ? prevPOMDocument
                  .getPomDocument()
                  .getRootElement()
                  .element("parent")
                  .element("artifactId")
                  .getText()
              : null;

      if (myArtifactId == null || prevParentArtifactId == null) {
        LOGGER.warn(
            "Missing previous mine or parent: " + myArtifactId + " / " + prevParentArtifactId);
        break;
      }

      if (!myArtifactId.equals(prevParentArtifactId)) {
        LOGGER.warn("Previous doesn't match: " + myArtifactId + " / " + prevParentArtifactId);
        break;
      }

      parentPomFiles.add(newPomFile);
      prevPOMDocument = newPomFile;

      Element newRelativePathElement =
          newPomFile.getPomDocument().getRootElement().element("parent") != null
              ? newPomFile
                  .getPomDocument()
                  .getRootElement()
                  .element("parent")
                  .element("relativePath")
              : null;

      if (newRelativePathElement != null) {
        pomFileQueue.add(newRelativePathElement);
      }
    }

    return ProjectModelFactory.loadFor(pomFile, parentPomFiles);
  }

  private static File lastFile;

  private static Path resolvePath(File baseFile, String relativePath) {
    File parentDir = baseFile;

    if (parentDir.isFile()) {
      parentDir = parentDir.getParentFile();
    }

    File result = new File(new File(parentDir, relativePath).toURI().normalize().getPath());

    lastFile = result.isDirectory() ? result : result.getParentFile();

    return Paths.get(result.getAbsolutePath());
  }

  private static String fixPomRelativePath(String text) {
    if (text == null) {
      return "";
    }

    String name = new File(text).getName();

    if (name.indexOf('.') == -1) {
      return text + "/pom.xml";
    }

    return text;
  }

  private static boolean isRelative(String path) {
    if (RE_WINDOWS_PATH.matcher(path).matches()) {
      return false;
    }

    return !(path.startsWith("/") || path.startsWith("~"));
  }

  private static List<File> getParentPoms(File originalFile) throws ModelBuildingException {
    EmbedderFacade.EmbedderFacadeResponse embedderFacadeResponse =
        EmbedderFacade.invokeEmbedder(
            new EmbedderFacade.EmbedderFacadeRequest(true, null, originalFile, null, null));

    ModelBuildingResult res = embedderFacadeResponse.getModelBuildingResult();

    List<Model> rawModels = new ArrayList<>();
    for (String modelId : res.getModelIds()) {
      Model rawModel = res.getRawModel(modelId);
      if (rawModel != null) {
        rawModels.add(rawModel);
      }
    }

    List<File> parentPoms = new ArrayList<>();
    if (rawModels.size() > 1) {
      for (int i = 1; i < rawModels.size(); i++) {
        Model rawModel = rawModels.get(i);
        if (rawModel != null) {
          File pomFile = rawModel.getPomFile();
          if (pomFile != null) {
            parentPoms.add(pomFile);
          }
        }
      }
    }

    return parentPoms;
  }
}
