package io.codemodder.codemods;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import io.codemodder.testutils.CodemodTestMixin;
import io.codemodder.testutils.Metadata;
import java.io.IOException;
import java.io.StringReader;
import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

@Metadata(
    codemodType = HardenXMLReaderCodemod.class,
    testResourceDir = "harden-xmlreader",
    dependencies = {})
final class HardenXMLReaderCodemodTest implements CodemodTestMixin {

  @Test
  void verify_insecure_xmlreader_pattern() throws SAXException, IOException {
    XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
    StringReader sr =
        new StringReader(
            """
                        <?xml version="1.0"?>
                        <book>
                            <title>My Book</title>
                            <author>John Doe</author>
                        </book>
                        """);
    ContentHandler handler = mock(ContentHandler.class);
    reader.parse(new InputSource(sr));
    verify(handler, times(0)).skippedEntity(anyString());
  }

  @Test
  void verify_proposed_fix_prevents_entity_resolution() throws SAXException, IOException {
    StringReader evilXml =
        new StringReader(
            """
                <!DOCTYPE foo [
                    <!ENTITY xxe SYSTEM "file:///etc/passwd">
                ]>
                <book>
                    <title>My Book</title>
                    <author>&xxe;</author>
                </book>
                """);

    XMLReader reader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
    reader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
    reader.setFeature("http://xml.org/sax/features/external-general-entities", false);
    ContentHandler handler = mock(ContentHandler.class);
    reader.setContentHandler(handler);
    reader.parse(new InputSource(evilXml)); // shouldn't throw exceptions
    verify(handler).skippedEntity(eq("xxe"));
  }
}
