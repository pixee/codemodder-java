package io.openpixee.maven.operator

import org.apache.commons.lang3.StringUtils
import org.mozilla.universalchardet.UniversalDetector
import java.io.StringWriter
import java.nio.charset.Charset
import java.util.BitSet
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
class FormatCommand : AbstractSimpleCommand() {
    /**
     * Preamble Contents are stored here
     */
    private var preamble: String = ""

    /**
     * Afterword - if needed
     */
    private var suffix: String = ""

    /**
     * StAX InputFactory
     */
    private val inputFactory = XMLInputFactory.newInstance()

    /**
     * StAX OutputFactory
     */
    private val outputFactory = XMLOutputFactory.newInstance()

    override fun execute(c: ProjectModel): Boolean {
        parseXmlAndCharset(c)

        parseLineEndings(c)

        c.endl = parseLineEndings(c)
        c.indent = guessIndent(c)

        return super.execute(c)
    }

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

    private fun findSingleElementMatchesFrom(pomDocument: String) =
        RE_EMPTY_ELEMENT.findAll(pomDocument).map {
            it.range.first to MatchData(
                range = it.range,
                content = it.value,
                elementName = ((it.groups[1]?.value ?: it.groups[2]?.value)!!)
            )
        }.sortedByDescending { it.first }.toMap(LinkedHashMap())

    private fun guessIndent(c: ProjectModel): String {
        val eventReader = inputFactory.createXMLEventReader(c.originalPom.inputStream())

        val indent = " "
        val freqMap: MutableMap<Int, Int> = mutableMapOf()

        /**
         * Parse, while grabbing whitespace sequences and counting
         */
        while (eventReader.hasNext()) {
            val event = eventReader.nextEvent()

            if (event is Characters) {
                if (StringUtils.isWhitespace(event.asCharacters().data)) {
                    val patterns = event.asCharacters().data.split(*LINE_ENDINGS.toTypedArray())

                    /**
                     * Updates space frequencies
                     */
                    patterns
                        .filter { it.isNotEmpty() }
                        .filter { StringUtils.isAllBlank(it) }
                        .map { it to it.length }
                        .forEach {
                            freqMap.merge(it.second, 1) { a, b -> a + b}
                        }
                }
            }
        }

        val indentLength = freqMap.entries.minBy { it.key }.key

        return StringUtils.repeat(indent, indentLength)
    }

    private fun parseLineEndings(c: ProjectModel): String {
        val str = String(c.originalPom.inputStream().readBytes(), c.charset)

        return LINE_ENDINGS.associateWith { str.split(it).size }
            .maxBy { it.value }
            .key
    }

    private fun parseXmlAndCharset(c: ProjectModel) {
        /**
         * Performs a StAX Parsing to Grab the first element
         */
        val eventReader = inputFactory.createXMLEventReader(c.originalPom.inputStream())

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

                preamble = c.originalPom.toString(c.charset).substring(0, offset)

                break
            }

            if (!eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        if (null == charset) {
            val detectedCharsetName = UniversalDetector.detectCharset(c.originalPom.inputStream())

            charset = Charset.forName(detectedCharsetName)
        }

        c.charset = charset!!

        val lastLine = String(c.originalPom, c.charset)

        val lastLineTrimmed = lastLine.trimEnd()

        this.suffix = lastLine.substring(lastLineTrimmed.length)

    }

    /**
     * When doing the opposite, render the XML using the optionally supplied encoding (defaults to UTF8 obviously)
     * but apply the original formatting as well
     */
    override fun postProcess(c: ProjectModel): Boolean {
        var xmlRepresentation = c.resultPom.asXML().toString()

        val originalElementMap = elementBitSet(c.originalPom)
        val targetElementMap = elementBitSet(xmlRepresentation.toByteArray())

        // Let's find out the original empty elements from the original pom and store into a stack
        val elementsToReplace : MutableList<MatchData> = ArrayList<MatchData>().apply{
            val matches = findSingleElementMatchesFrom(c.originalPom.toString(c.charset)).values

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
            xmlRepresentation.toByteArray(c.charset).inputStream()
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
                    this.preamble + xmlRepresentation.substring(offset) + this.suffix

                break
            }

            /**
             * This code shouldn't be unreachable at all
             */
            if (!eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        /**
         * Serializes it back
         */
        c.resultPomBytes = xmlRepresentation.toByteArray(c.charset)

        return super.postProcess(c)
    }

    companion object {
        val LINE_ENDINGS = setOf("\r\n", "\n", "\r")

        val RE_EMPTY_ELEMENT = Regex("""<(\p{Alnum}+)></\1>|<(\p{Alnum}+)\s*/>""")
    }
}