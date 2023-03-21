package io.codemodder.codemods;

import io.codemodder.Codemod;
import io.codemodder.ReviewGuidance;
import io.codemodder.RuleSarif;
import io.codemodder.providers.sarif.semgrep.SemgrepScan;
import io.codemodder.providers.sarif.semgrep.SemgrepXMLElementChanger;
import javax.inject.Inject;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;

/** Removes all &lt;http-method&gt; XML elements from files named web.xml. */
@Codemod(
    id = "pixee:java/fix-verb-tampering",
    author = "arshan@pixee.ai",
    reviewGuidance = ReviewGuidance.MERGE_WITHOUT_REVIEW)
public final class VerbTamperingCodemod extends SemgrepXMLElementChanger {

  @Inject
  public VerbTamperingCodemod(@SemgrepScan(ruleId = "verb-tampering") final RuleSarif ruleSarif) {
    super(ruleSarif);
  }

  @Override
  protected void onSemgrepResultFound(
      final XMLEventReader xmlReader, final XMLEventWriter xmlWriter, final StartElement element)
      throws XMLStreamException {
    // skip the inner text element which contains the HTTP method
    xmlReader.nextEvent();
    // skip the </http-method> closing tag
    xmlReader.nextEvent();
  }
}
