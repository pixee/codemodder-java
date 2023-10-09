package io.codemodder.plugins.maven.operator;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

/** Command Class to Short-Circuit/Discard Processing when no pom changes were made */
public class DiscardFormatCommand extends AbstractCommand {

  private static DiscardFormatCommand instance;

  private DiscardFormatCommand() {
    // Private constructor to prevent instantiation.
  }

  public static DiscardFormatCommand getInstance() {
    if (instance == null) {
      instance = new DiscardFormatCommand();
    }
    return instance;
  }

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
