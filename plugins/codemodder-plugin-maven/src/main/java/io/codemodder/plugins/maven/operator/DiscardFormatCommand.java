package io.codemodder.plugins.maven.operator;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

/** Command Class to Short-Circuit/Discard Processing when no pom changes were made */
class DiscardFormatCommand extends AbstractCommand {

  DiscardFormatCommand() {}

  /**
   * Post-processes the ProjectModel to check if any POM changes were made by comparing the original
   * POMs with the modified ones. If no differences are found, the processing is short-circuited,
   * and the modified POMs are discarded.
   *
   * @param pm ProjectModel containing project information.
   * @return true if the processing should be discarded due to no changes; false otherwise.
   * @throws XMLStreamException if there is an issue with XML stream processing.
   */
  @Override
  public boolean postProcess(ProjectModel pm) throws XMLStreamException {
    boolean mustSkip = false;

    for (POMDocument pomFile : pm.allPomFiles()) {
      Source originalDoc = Input.fromString(new String(pomFile.getOriginalPom())).build();
      Source modifiedDoc = Input.fromString(pomFile.getResultPom().asXML()).build();

      Diff diff =
          DiffBuilder.compare(originalDoc)
              .withTest(modifiedDoc)
              .ignoreWhitespace()
              .ignoreComments()
              .ignoreElementContentWhitespace()
              .checkForSimilar()
              .build();

      boolean hasDifferences = diff.hasDifferences();

      if (!(pm.isModifiedByCommand() || hasDifferences)) {
        pomFile.setResultPomBytes(pomFile.getOriginalPom());
        mustSkip = true;
      }
    }

    /** Triggers early abandonment */
    if (mustSkip) {
      return true;
    }

    return super.postProcess(pm);
  }
}
