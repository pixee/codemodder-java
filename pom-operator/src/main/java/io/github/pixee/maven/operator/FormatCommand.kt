package io.github.pixee.maven.operator

import org.apache.commons.lang3.StringUtils
import org.mozilla.universalchardet.UniversalDetector
import java.io.StringWriter
import java.nio.charset.Charset
import java.util.*
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.events.Characters
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartDocument
import javax.xml.stream.events.StartElement

/**
 * Data Class used to keep track of matches (ranges, content, referring tag name)
 */
data class MatchData(
    val range: IntRange,
    val content: String,
    val elementName: String
)

/**
 * This Command handles Formatting - particularly storing the original document preamble (the Processing Instruction and the first XML Element contents),
 * which are the only ones which are tricky to format (due to element and its attributes being freeform - thus formatting lost when serializing the DOM
 * and the PI being completely optional for the POM Document)
 */
class FormatCommand : AbstractCommand() {
    /**
     * StAX InputFactory
     */
    private val inputFactory = XMLInputFactory.newInstance()

    /**
     * StAX OutputFactory
     */
    private val outputFactory = XMLOutputFactory.newInstance()

    override fun execute(pm: ProjectModel): Boolean {
        for (pomFile in pm.allPomFiles) {
            parseXmlAndCharset(pomFile)

            pomFile.endl = parseLineEndings(pomFile)
            pomFile.indent = guessIndent(pomFile)
        }

        return super.execute(pm)
    }

    /**
     * This one is quite fun yet important. Let me explain:
     *
     * The DOM doesn't track records if empty elements are either `<element>` or `<element/>`. Therefore we need to scan all ocurrences of
     * singleton elements.
     *
     * Therefore we use a bitSet to keep track of each element and offset, scanning it forward
     * when serializing we pick backwards and rewrite tags accordingly
     *
     * @param doc Raw Document Bytes
     * @see RE_EMPTY_ELEMENT
     * @return bitSet of
     *
     */
    private fun elementBitSet(doc: ByteArray): BitSet {
        val result = BitSet()
        val eventReader = inputFactory.createXMLEventReader(doc.inputStream())
        val eventContent = StringWriter()
        val xmlEventWriter = outputFactory.createXMLEventWriter(eventContent)

        while (eventReader.hasNext()) {
            val next = eventReader.nextEvent()

            if (next is StartElement || next is EndElement) {
                val startIndex = next.location.characterOffset

                eventContent.buffer.setLength(0)

                xmlEventWriter.add(next)
                xmlEventWriter.flush()

                val endIndex = startIndex + eventContent.buffer.length

                result.set(startIndex, startIndex + endIndex)
            }
        }

        return result
    }

    /**
     * Returns a reverse-ordered list of all the single element matches from the pom document
     * raw string
     *
     * this is important so we can mix and match offsets and apply formatting accordingly
     *
     * @param xmlDocumentString Rendered POM Document Contents (string-formatted)
     * @return map of (index, matchData object) reverse ordered
     */
    private fun findSingleElementMatchesFrom(xmlDocumentString: String) =
        RE_EMPTY_ELEMENT.findAll(xmlDocumentString).map {
            it.range.first to MatchData(
                range = it.range,
                content = it.value,
                elementName = ((it.groups[1]?.value ?: it.groups[2]?.value)!!)
            )
        }.sortedByDescending { it.first }.toMap(LinkedHashMap())

    /**
     * Guesses the indent character (spaces / tabs) and length from the original document
     * formatting settings
     *
     * @param pomFile (project model) where it takes its input pom
     * @return indent string
     */
    private fun guessIndent(pomFile: POMDocument): String {
        val eventReader = inputFactory.createXMLEventReader(pomFile.originalPom.inputStream())

        val freqMap: MutableMap<Int, Int> = mutableMapOf()
        val charFreqMap: MutableMap<Char, Int> = mutableMapOf()

        /**
         * Parse, while grabbing whitespace sequences and examining it
         */
        while (eventReader.hasNext()) {
            val event = eventReader.nextEvent()

            if (event is Characters) {
                if (StringUtils.isWhitespace(event.asCharacters().data)) {
                    val patterns = event.asCharacters().data.split(*LINE_ENDINGS.toTypedArray())

                    /**
                     * Updates space / character frequencies found
                     */
                    val blankPatterns = patterns
                        .filter { it.isNotEmpty() }
                        .filter { StringUtils.isAllBlank(it) }

                    blankPatterns
                        .map { it to it.length }
                        .forEach {
                            freqMap.merge(it.second, 1) { a, b -> a + b }
                        }

                    blankPatterns.map { it[0] }
                        .forEach {
                            charFreqMap.merge(it, 1) { a, b ->
                                a + b
                            }
                        }
                }
            }
        }

        /**
         * Assign most frequent indent char
         */
        val indentCharacter: Char = charFreqMap.entries.maxBy { it.value }.key

        /**
         * Casts as a String
         */
        val indentcharacterAsString = String(charArrayOf(indentCharacter))

        /**
         * Picks the length
         */
        val indentLength = freqMap.entries.minBy { it.key }.key

        /**
         * Builds the standard indent string (length vs char)
         */
        val indentString = StringUtils.repeat(indentcharacterAsString, indentLength)

        /**
         * Returns it
         */
        return indentString
    }

    private fun parseLineEndings(pomFile: POMDocument): String {
        val str = String(pomFile.originalPom.inputStream().readBytes(), pomFile.charset)

        return LINE_ENDINGS.associateWith { str.split(it).size }
            .maxBy { it.value }
            .key
    }

    private fun parseXmlAndCharset(pomFile: POMDocument) {
        /**
         * Performs a StAX Parsing to Grab the first element
         */
        val eventReader = inputFactory.createXMLEventReader(pomFile.originalPom.inputStream())

        var charset: Charset? = null

        /**
         * Parse, while grabbing its preamble and encoding
         */
        while (true) {
            val event = eventReader.nextEvent()

            if (event.isStartDocument && (event as StartDocument).encodingSet()) {
                /**
                 * Processing Instruction Found - Store its Character Encoding
                 */
                charset = Charset.forName(event.characterEncodingScheme)
            } else if (event.isEndElement) {
                /**
                 * First End of Element ("Tag") found - store its offset
                 */
                val endElementEvent = (event as EndElement)

                val offset = endElementEvent.location.characterOffset

                pomFile.preamble =
                    pomFile.originalPom.toString(pomFile.charset).substring(0, offset)

                break
            }

            if (!eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        if (null == charset) {
            val detectedCharsetName =
                UniversalDetector.detectCharset(pomFile.originalPom.inputStream())

            charset = Charset.forName(detectedCharsetName)
        }

        pomFile.charset = charset!!

        val lastLine = String(pomFile.originalPom, pomFile.charset)

        val lastLineTrimmed = lastLine.trimEnd()

        pomFile.suffix = lastLine.substring(lastLineTrimmed.length)
    }

    /**
     * When doing the opposite, render the XML using the optionally supplied encoding (defaults to UTF8 obviously)
     * but apply the original formatting as well
     */
    override fun postProcess(pm: ProjectModel): Boolean {
        for (pomFile in pm.allPomFiles) {
            /**
             * Serializes it back
             */
            val content = serializePomFile(pomFile)

            pomFile.resultPomBytes = content
        }

        return super.postProcess(pm)
    }

    /**
     * Serialize a POM Document
     *
     * @param pom pom document
     * @return bytes for the pom document
     */
    private fun serializePomFile(pom: POMDocument): ByteArray {
        // Generate a String representation. We'll need to patch it up and apply back
        // differences we recored previously on the pom (see the pom member variables)
        var xmlRepresentation = pom.resultPom.asXML().toString()

        val originalElementMap = elementBitSet(pom.originalPom)
        val targetElementMap = elementBitSet(xmlRepresentation.toByteArray())

        // Let's find out the original empty elements from the original pom and store into a stack
        val elementsToReplace: MutableList<MatchData> = ArrayList<MatchData>().apply {
            val matches =
                findSingleElementMatchesFrom(pom.originalPom.toString(pom.charset)).values

            val filteredMatches = matches.filter { originalElementMap[it.range.first] }

            this.addAll(filteredMatches)
        }

        // Lets to the replacements backwards on the existing, current pom
        val emptyElements = findSingleElementMatchesFrom(xmlRepresentation)
            .filter { targetElementMap[it.value.range.first] }

        emptyElements.forEach { (_, match) ->
            val nextMatch = elementsToReplace.removeFirst()

            xmlRepresentation = xmlRepresentation.replaceRange(match.range, nextMatch.content)
        }

        /**
         * We might need to replace the beginning of the POM with the same content
         * from the very beginning
         *
         * Grab the same initial offset from the formatted element like we did
         */
        val inputFactory = XMLInputFactory.newInstance()
        val eventReader = inputFactory.createXMLEventReader(
            xmlRepresentation.toByteArray(pom.charset).inputStream()
        )

        while (true) {
            val event = eventReader.nextEvent()

            if (event.isEndElement) {
                /**
                 * Apply the formatting and tweak its XML Representation
                 */
                val endElementEvent = (event as EndElement)

                val offset = endElementEvent.location.characterOffset

                xmlRepresentation =
                    pom.preamble + xmlRepresentation.substring(offset) + pom.suffix

                break
            }

            /**
             * This code shouldn't be unreachable at all
             */
            if (!eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        /**
         * Serializes it back from (string to ByteArray)
         */
        val serializedContent = xmlRepresentation.toByteArray(pom.charset)

        return serializedContent
    }

    companion object {
        val LINE_ENDINGS = setOf("\r\n", "\n", "\r")

        val RE_EMPTY_ELEMENT = Regex("""<(\p{Alnum}+)></\1>|<(\p{Alnum}+)\s*/>""")
    }
}