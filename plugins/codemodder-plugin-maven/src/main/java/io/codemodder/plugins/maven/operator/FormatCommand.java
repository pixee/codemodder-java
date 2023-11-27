package io.codemodder.plugins.maven.operator;

import static io.github.pixee.security.XMLInputFactorySecurity.hardenFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.*;
import javax.xml.stream.events.*;
import kotlin.ranges.IntRange;
import kotlin.sequences.Sequence;
import kotlin.text.MatchGroupCollection;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Command handles Formatting - particularly storing the original document preamble (the
 * Processing Instruction and the first XML Element contents), which are the only ones which are
 * tricky to format (due to element and its attributes being freeform - thus formatting lost when
 * serializing the DOM and the PI being completely optional for the POM Document)
 */
class FormatCommand extends AbstractCommand {

  private static final Set<String> LINE_ENDINGS = new HashSet<>();
  private static final Regex RE_EMPTY_ELEMENT_NO_ATTRIBUTES;
  private static final Logger LOGGER = LoggerFactory.getLogger(FormatCommand.class);

  /** StAX InputFactory */
  private XMLInputFactory inputFactory = hardenFactory(XMLInputFactory.newInstance());

  /** StAX OutputFactory */
  private XMLOutputFactory outputFactory = XMLOutputFactory.newInstance();

  private List<MatchData> singleElementsWithAttributes = new ArrayList<>();

  static {
    LINE_ENDINGS.add("\r\n");
    LINE_ENDINGS.add("\n");
    LINE_ENDINGS.add("\r");

    RE_EMPTY_ELEMENT_NO_ATTRIBUTES =
        new Regex("<([\\p{Alnum}_\\-.]+)>\\s*</\\1>|<([\\p{Alnum}_\\-.]+)\\s*/>");
  }

  /**
   * Execute the formatting operation.
   *
   * @param pm The ProjectModel to operate on.
   * @return `true` if the formatting operation was successful, `false` otherwise.
   * @throws XMLStreamException if there is an issue with XML processing.
   * @throws IOException if there is an issue with I/O operations.
   * @throws URISyntaxException if there is an issue with URI syntax.
   */
  @Override
  public boolean execute(ProjectModel pm)
      throws XMLStreamException, IOException, URISyntaxException {
    for (POMDocument pomFile : pm.allPomFiles()) {
      parseXmlAndCharset(pomFile);

      pomFile.setEndl(parseLineEndings(pomFile));
      pomFile.setIndent(guessIndent(pomFile));
    }

    return super.execute(pm);
  }

  /**
   * Perform post-processing after formatting. When doing the opposite, render the XML using the
   * optionally supplied encoding (defaults to UTF8 obviously) but apply the original formatting as
   * well
   *
   * @param pm The ProjectModel to operate on.
   * @return `true` if the post-processing was successful, `false` otherwise.
   * @throws XMLStreamException if there is an issue with XML processing.
   */
  @Override
  public boolean postProcess(ProjectModel pm) throws XMLStreamException {
    for (POMDocument pomFile : pm.allPomFiles()) {
      /** Serializes it back */
      byte[] content = serializePomFile(pomFile);
      pomFile.setResultPomBytes(content);
    }
    return super.postProcess(pm);
  }

  /**
   * This one is quite fun yet important. Let me explain:
   *
   * <p>The DOM doesn't track records if empty elements are either `<element>` or `<element/>`.
   * Therefore we need to scan all ocurrences of singleton elements.
   *
   * <p>Therefore we use a bitSet to keep track of each element and offset, scanning it forward when
   * serializing we pick backwards and rewrite tags accordingly
   *
   * @param doc Raw Document Bytes
   * @see RE_EMPTY_ELEMENT_NO_ATTRIBUTES
   * @return bitSet of
   */
  private BitSet elementBitSet(byte[] doc) throws XMLStreamException {
    BitSet result = new BitSet();
    XMLEventReader eventReader = inputFactory.createXMLEventReader(new ByteArrayInputStream(doc));
    StringWriter eventContent = new StringWriter();
    XMLEventWriter xmlEventWriter = outputFactory.createXMLEventWriter(eventContent);

    while (eventReader.hasNext()) {
      XMLEvent next = eventReader.nextEvent();

      if (next instanceof StartElement || next instanceof EndElement) {
        int startIndex = next.getLocation().getCharacterOffset();

        eventContent.getBuffer().setLength(0);

        xmlEventWriter.add(next);
        xmlEventWriter.flush();

        int endIndex = startIndex + eventContent.getBuffer().length();

        result.set(startIndex, startIndex + endIndex);
      }
    }

    return result;
  }

  /**
   * A Slight variation on writeAsUnicode from stax which writes as a regex string so we could
   * rewrite its output
   */
  private String writeAsRegex(StartElement element) {
    StringWriter writer = new StringWriter();

    writer.write("<");
    writer.write(Pattern.quote(element.getName().getLocalPart()));

    Iterator<?> attrIter = element.getAttributes();
    while (attrIter.hasNext()) {
      Attribute attr = (Attribute) attrIter.next();

      writer.write("\\s+");

      writer.write(Pattern.quote(attr.getName().getLocalPart()));
      writer.write("=[\\\"\']");
      writer.write(Pattern.quote(attr.getValue()));
      writer.write("[\\\"\']");
    }
    writer.write("\\s*\\/>");

    return writer.toString();
  }

  private String parseLineEndings(POMDocument pomFile) throws IOException {
    InputStream inputStream = new ByteArrayInputStream(pomFile.getOriginalPom());
    byte[] bytes = inputStream.readAllBytes();
    String str = new String(bytes, pomFile.getCharset());

    Map<String, Integer> lineEndingCounts = new HashMap<>();
    for (String lineEnding : LINE_ENDINGS) {
      lineEndingCounts.put(lineEnding, str.split(lineEnding).length);
    }

    return Collections.max(lineEndingCounts.entrySet(), Map.Entry.comparingByValue()).getKey();
  }

  /**
   * Guesses the indent character (spaces / tabs) and length from the original document formatting
   * settings
   *
   * @param pomFile (project model) where it takes its input pom
   * @return indent string
   */
  private String guessIndent(POMDocument pomFile) throws XMLStreamException {
    InputStream inputStream = new ByteArrayInputStream(pomFile.getOriginalPom());
    XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);

    Map<Integer, Integer> freqMap = new HashMap<>();
    Map<Character, Integer> charFreqMap = new HashMap<>();

    /** Parse, while grabbing whitespace sequences and examining it */
    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      if (event instanceof Characters) {
        Characters characters = (Characters) event;
        String data = characters.getData();

        if (StringUtils.isWhitespace(data)) {
          String lineEndingsPattern = String.join("|", LINE_ENDINGS.toArray(new String[0]));

          String[] patterns = data.split(lineEndingsPattern);

          /** Updates space / character frequencies found */
          for (String pattern : patterns) {
            if (!pattern.isEmpty() && StringUtils.isAllBlank(pattern)) {
              int length = pattern.length();
              freqMap.merge(length, 1, Integer::sum);

              char firstChar = pattern.charAt(0);
              charFreqMap.merge(firstChar, 1, Integer::sum);
            }
          }
        }
      }
    }

    // Assign the most frequent indent char
    char indentCharacter = getMostFrequentIndentChar(charFreqMap);

    // Cast it as a String
    String indentCharacterAsString = String.valueOf(indentCharacter);

    // Pick the length
    int indentLength = getMinimumIndentLength(freqMap);

    // Build the standard indent string (length vs char)
    String indentString = StringUtils.repeat(indentCharacterAsString, indentLength);

    // Return it
    return indentString;
  }

  private char getMostFrequentIndentChar(Map<Character, Integer> charFreqMap) {
    char mostFrequentChar = '\0';
    int maxFrequency = Integer.MIN_VALUE;

    for (Map.Entry<Character, Integer> entry : charFreqMap.entrySet()) {
      if (entry.getValue() > maxFrequency) {
        maxFrequency = entry.getValue();
        mostFrequentChar = entry.getKey();
      }
    }

    return mostFrequentChar;
  }

  private int getMinimumIndentLength(Map<Integer, Integer> freqMap) {
    int minIndentLength = Integer.MAX_VALUE;

    for (Map.Entry<Integer, Integer> entry : freqMap.entrySet()) {
      if (entry.getKey() < minIndentLength) {
        minIndentLength = entry.getKey();
      }
    }

    return minIndentLength;
  }

  private void parseXmlAndCharset(POMDocument pomFile) throws XMLStreamException, IOException {
    InputStream inputStream = new ByteArrayInputStream(pomFile.getOriginalPom());

    /** Performs a StAX Parsing to Grab the first element */
    XMLEventReader eventReader = inputFactory.createXMLEventReader(inputStream);

    Charset charset = null;
    /** Parse, while grabbing its preamble and encoding */
    int elementIndex = 0;
    boolean mustTrack = false;
    boolean hasPreamble = false;
    int elementStart = 0;
    int elementEnd = 0;
    List<XMLEvent> prevEvents = new ArrayList<>();

    while (eventReader.hasNext()) {
      XMLEvent event = eventReader.nextEvent();

      if (event.isStartDocument() && ((StartDocument) event).encodingSet()) {
        /** Processing Instruction Found - Store its Character Encoding */
        charset = Charset.forName(((StartDocument) event).getCharacterEncodingScheme());
      } else if (event.isStartElement()) {
        StartElement asStartElement = event.asStartElement();

        String name = asStartElement.getName().getLocalPart();

        List<Attribute> attributes = new ArrayList<>();
        Iterator<?> attrIter = asStartElement.getAttributes();
        while (attrIter.hasNext()) {
          attributes.add((Attribute) attrIter.next());
        }

        if (elementIndex > 0 && !attributes.isEmpty()) {
          // record this guy
          mustTrack = true;

          Characters lastCharacterEvent = null;
          for (int i = prevEvents.size() - 1; i >= 0; i--) {
            if (prevEvents.get(i).isCharacters()) {
              lastCharacterEvent = prevEvents.get(i).asCharacters();
              break;
            }
          }

          if (lastCharacterEvent != null) {
            elementStart =
                lastCharacterEvent.getLocation().getCharacterOffset()
                    - lastCharacterEvent.getData().length();
          }
        } else if (mustTrack) { // turn it off
          mustTrack = false;
        }

        elementIndex++;
      } else if (event.isEndElement()) {
        /** First End of Element ("Tag") found - store its offset */
        EndElement endElementEvent = event.asEndElement();

        Location location = endElementEvent.getLocation();

        int offset = location.getCharacterOffset();

        if (mustTrack) {
          mustTrack = false;
          String localPart = event.asEndElement().getName().getLocalPart();

          String originalPomCharsetString =
              new String(pomFile.getOriginalPom(), pomFile.getCharset());

          String untrimmedOriginalContent =
              originalPomCharsetString.substring(elementStart, offset);

          String trimmedOriginalContent = untrimmedOriginalContent.trim();

          int realElementStart =
              originalPomCharsetString.indexOf(trimmedOriginalContent, elementStart);

          IntRange contentRange =
              new IntRange(
                  realElementStart, realElementStart + 1 + trimmedOriginalContent.length());

          String contentRe = writeAsRegex(getLastStartElement(prevEvents));

          Regex modifiedContentRE = new Regex(contentRe);

          singleElementsWithAttributes.add(
              new MatchData(
                  contentRange, trimmedOriginalContent, localPart, true, modifiedContentRE));
        }

        mustTrack = false;

        /** Sets Preamble - keeps parsing anyway */
        if (!hasPreamble) {
          pomFile.setPreamble(
              new String(pomFile.getOriginalPom(), pomFile.getCharset()).substring(0, offset));
          hasPreamble = true;
        }
      }

      prevEvents.add(event);

      while (prevEvents.size() > 4) {
        prevEvents.remove(0);
      }

      if (!eventReader.hasNext())
        if (!hasPreamble) throw new IllegalStateException("Couldn't find document start");
    }

    if (null == charset) {
      InputStream inputStream2 = new ByteArrayInputStream(pomFile.getOriginalPom());
      String detectedCharsetName = UniversalDetector.detectCharset(inputStream2);

      charset = Charset.forName(detectedCharsetName);
    }

    pomFile.setCharset(charset);

    String lastLine = new String(pomFile.getOriginalPom(), pomFile.getCharset());

    String lastLineTrimmed = lastLine.replaceAll("\\s+$", "");

    pomFile.setSuffix(lastLine.substring(lastLineTrimmed.length()));
  }

  private StartElement getLastStartElement(List<XMLEvent> prevEvents) {
    for (int i = prevEvents.size() - 1; i >= 0; i--) {
      XMLEvent event = prevEvents.get(i);
      if (event.isStartElement()) {
        return (StartElement) event;
      }
    }
    return null; // Handle the case where no StartElement event is found.
  }

  /**
   * Returns a reverse-ordered list of all the single element matches from the pom document raw
   * string
   *
   * <p>this is important so we can mix and match offsets and apply formatting accordingly
   *
   * @param xmlDocumentString Rendered POM Document Contents (string-formatted)
   * @return map of (index, matchData object) reverse ordered
   */
  private LinkedHashMap<Integer, MatchData> findSingleElementMatchesFrom(String xmlDocumentString) {
    Sequence<MatchResult> allFoundMatchesSequence =
        RE_EMPTY_ELEMENT_NO_ATTRIBUTES.findAll(xmlDocumentString, 0);

    List<MatchData> emptyMappedTags = new ArrayList<>();

    Iterator<MatchResult> iterator = allFoundMatchesSequence.iterator();
    while (iterator.hasNext()) {
      MatchResult matchResult = iterator.next();
      MatchGroupCollection groups = matchResult.getGroups();
      String value1 = (groups.get(1) != null) ? groups.get(1).getValue() : null;
      String value2 = (groups.get(2) != null) ? groups.get(2).getValue() : null;
      MatchData matchDataJ =
          new MatchData(
              matchResult.getRange(),
              matchResult.getValue(),
              (value1 != null) ? value1 : value2,
              false,
              null);
      emptyMappedTags.add(matchDataJ);
    }

    List<Pair<Integer, MatchData>> allTags =
        emptyMappedTags.stream()
            .flatMap(data -> Stream.of(data))
            .map(data -> new Pair<>(data.getRange().getFirst(), data))
            .collect(Collectors.toList());

    allTags.sort(Comparator.comparing(Pair::getFirst, Comparator.reverseOrder()));

    LinkedHashMap<Integer, MatchData> linkedHashMap = new LinkedHashMap<>();

    for (Pair<Integer, MatchData> pair : allTags) {
      linkedHashMap.put(pair.getFirst(), pair.getSecond());
    }

    return linkedHashMap;
  }

  private List<MatchData> getElementsToReplace(BitSet originalElementMap, POMDocument pom) {
    // Let's find out the original empty elements from the original pom and store them in a stack
    List<MatchData> elementsToReplace = new ArrayList<>();
    Map<Integer, MatchData> singleElementMatches =
        findSingleElementMatchesFrom(new String(pom.getOriginalPom(), pom.getCharset()));

    for (MatchData match : singleElementMatches.values()) {
      if (!match.getHasAttributes() && originalElementMap.get(match.getRange().getFirst())) {
        elementsToReplace.add(match);
      }
    }

    return elementsToReplace;
  }

  private Map<Integer, MatchData> getEmptyElements(
      BitSet targetElementMap, String xmlRepresentation) {
    LinkedHashMap<Integer, MatchData> emptyElements = new LinkedHashMap<>();
    for (Map.Entry<Integer, MatchData> entry :
        findSingleElementMatchesFrom(xmlRepresentation).entrySet()) {
      Integer key = entry.getKey();
      MatchData value = entry.getValue();

      if (targetElementMap.get(value.getRange().getFirst())) {
        emptyElements.put(key, value);
      }
    }

    return emptyElements;
  }

  private String replaceRange(String xmlRepresentation, IntRange range, String replacement) {
    StringBuilder sb = new StringBuilder();
    sb.append(xmlRepresentation.substring(0, range.getStart()));
    sb.append(replacement);
    sb.append(xmlRepresentation.substring(range.getEndInclusive() + 1, xmlRepresentation.length()));
    return sb.toString();
  }

  /**
   * Serialize a POM Document
   *
   * @param pom pom document
   * @return bytes for the pom document
   */
  private byte[] serializePomFile(POMDocument pom) throws XMLStreamException {
    // Generate a String representation. We'll need to patch it up and apply back
    // differences we recorded previously on the pom (see the pom member variables)
    String xmlRepresentation = pom.getResultPom().asXML().toString();

    BitSet originalElementMap = elementBitSet(pom.getOriginalPom());
    BitSet targetElementMap = elementBitSet(xmlRepresentation.getBytes());

    // Let's find out the original empty elements from the original pom and store into a stack
    List<MatchData> elementsToReplace = getElementsToReplace(originalElementMap, pom);

    // Lets to the replacements backwards on the existing, current pom
    Map<Integer, MatchData> emptyElements = getEmptyElements(targetElementMap, xmlRepresentation);

    for (Map.Entry<Integer, MatchData> entry : emptyElements.entrySet()) {
      Integer key = entry.getKey();
      MatchData match = entry.getValue();

      MatchData nextMatch = elementsToReplace.remove(0);

      xmlRepresentation = replaceRange(xmlRepresentation, match.getRange(), nextMatch.getContent());
    }

    int lastIndex = 0;

    singleElementsWithAttributes.sort(
        Comparator.comparingInt(matchDataJ -> matchDataJ.getRange().getFirst()));

    for (MatchData match : singleElementsWithAttributes) {
      MatchResult representationMatch =
          match.getModifiedContent().find(xmlRepresentation, lastIndex);

      if (null == representationMatch) {
        LOGGER.warn("Failure on quoting: {}", match);
      } else {
        xmlRepresentation =
            replaceRange(xmlRepresentation, representationMatch.getRange(), match.getContent());
        lastIndex = representationMatch.getRange().getFirst() + match.getContent().length();
      }
    }

    /**
     * We might need to replace the beginning of the POM with the same content from the very
     * beginning
     *
     * <p>Grab the same initial offset from the formatted element like we did
     */
    XMLInputFactory inputFactory = hardenFactory(XMLInputFactory.newInstance());
    XMLEventReader eventReader =
        inputFactory.createXMLEventReader(
            new ByteArrayInputStream(xmlRepresentation.getBytes(pom.getCharset())));

    while (true) {
      XMLEvent event = eventReader.nextEvent();

      if (event.isEndElement()) {
        /** Apply the formatting and tweak its XML Representation */
        EndElement endElementEvent = (EndElement) event;
        int offset = endElementEvent.getLocation().getCharacterOffset();
        xmlRepresentation =
            pom.getPreamble() + xmlRepresentation.substring(offset) + pom.getSuffix();
        break;
      }
      /** This code shouldn't be unreachable at all */
      if (!eventReader.hasNext()) {
        throw new IllegalStateException("Couldn't find document start");
      }
    }

    /** Serializes it back from (string to ByteArray) */
    byte[] serializedContent = xmlRepresentation.getBytes(pom.getCharset());

    return serializedContent;
  }
}
