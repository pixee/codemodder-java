package io.codemodder.plugins.maven.operator;

import static org.assertj.core.api.Assertions.assertThat;

import fun.mike.dmp.DiffMatchPatch;
import fun.mike.dmp.Patch;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import javax.xml.stream.XMLStreamException;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

class AbstractTestBase {
  protected static final Logger LOGGER = LoggerFactory.getLogger(POMOperatorTest.class);

  static enum OperationType {
    MODIFY,
    INSERT,
    UPDATE
  }

  protected Path getResource(String name) throws URISyntaxException {
    return Paths.get(this.getClass().getResource(name).toURI());
  }

  Path getResourceAsPath(String name) throws URISyntaxException {
    Path resourceUrl = getResource(name);
    return resourceUrl;
  }

  protected ProjectModel performAndAssertModifyPomOperation(String name, ProjectModelFactory pmf)
      throws Exception {
    return performAndAssertPomOperation(name, pmf.build(), OperationType.MODIFY);
  }

  protected ProjectModel performAndAssertInsertPomOperation(String name, ProjectModelFactory pmf)
      throws Exception {
    return performAndAssertPomOperation(name, pmf.build(), OperationType.INSERT);
  }

  protected ProjectModel performAndAssertUpdatePomOperation(String name, ProjectModelFactory pmf)
      throws Exception {
    return performAndAssertPomOperation(name, pmf.build(), OperationType.UPDATE);
  }

  protected ProjectModel performAndAssertPomOperation(
      String testName, ProjectModel context, final OperationType operationType) throws Exception {

    String resultFile = "pom-" + testName + "-result.xml";
    URL resource = AbstractTestBase.class.getClass().getResource(resultFile);

    if (resource != null) {
      Document outcome = new SAXReader().read(resource);
      performPomOperation(operationType, context);
      // "Expected and outcome have differences"
      assertThat(getXmlDifferences(context.getPomFile().getResultPom(), outcome).hasDifferences())
          .isFalse();
    } else {
      Path resultFilePath =
          Paths.get(
              "src/test/resources/",
              AbstractTestBase.class.getPackage().getName().replace(".", "/"),
              resultFile);

      performPomOperation(operationType, context);

      byte[] resultPomBytes = context.getPomFile().getResultPomBytes();

      try {
        Files.write(resultFilePath, resultPomBytes);
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return context;
  }

  private void performPomOperation(
      final OperationType operationType, final ProjectModel projectModel)
      throws XMLStreamException, URISyntaxException, IOException {
    switch (operationType) {
      case INSERT -> POMOperator.insertOnly(projectModel);
      case MODIFY -> POMOperator.modify(projectModel);
      case UPDATE -> POMOperator.updateOnly(projectModel);
      default -> throw new IllegalArgumentException("Invalid operation type: " + operationType);
    }
  }

  static Diff getXmlDifferences(Document original, Document modified) {
    String originalDoc = original.asXML();
    String modifiedDoc = modified.asXML();

    Diff diff =
        DiffBuilder.compare(Input.fromString(originalDoc))
            .withTest(Input.fromString(modifiedDoc))
            .ignoreWhitespace()
            .checkForSimilar()
            .build();

    LOGGER.debug("diff: " + diff);

    return diff;
  }

  protected String getTextDifferences(Document pomDocument, Document resultPom)
      throws UnsupportedEncodingException {
    String pomDocumentAsString = pomDocument.asXML();
    String resultPomAsString = resultPom.asXML();

    DiffMatchPatch dmp = new DiffMatchPatch();
    LinkedList<Patch> patches = dmp.patch_make(pomDocumentAsString, resultPomAsString);
    String patchText = dmp.patch_toText(patches);

    return URLDecoder.decode(patchText, "utf-8");
  }
}
