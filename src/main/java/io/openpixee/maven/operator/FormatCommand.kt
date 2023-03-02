package io.openpixee.maven.operator

import org.apache.commons.lang3.StringUtils
import org.dom4j.Comment
import org.dom4j.Element
import org.dom4j.Text
import org.dom4j.VisitorSupport
import org.mozilla.universalchardet.UniversalDetector
import java.nio.charset.Charset
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.Characters
import javax.xml.stream.events.StartDocument
import javax.xml.stream.events.StartElement

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

    override fun execute(c: ProjectModel): Boolean {
        storeOriginalElements(c)

        parseXmlAndCharset(c)

        parseLineEndings(c)

        c.endl = parseLineEndings(c)
        c.indent = guessIndent(c)

        return super.execute(c)
    }

    private fun storeOriginalElements(c: ProjectModel) {
        val elementSet : MutableSet<Int> = mutableSetOf()

        c.resultPom.accept(object : VisitorSupport() {
            override fun visit(node: Element?) {
                elementSet.add(System.identityHashCode(node!!))

                super.visit(node)
            }

            override fun visit(node: Comment?) {
                elementSet.add(System.identityHashCode(node!!))

                super.visit(node)
            }

            override fun visit(node: Text?) {
                elementSet.add(System.identityHashCode(node!!))

                super.visit(node)
            }
        })

        c.originalElements = elementSet.toSet()
    }

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
                        .filter { it.length != 0 }
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

        return LINE_ENDINGS
            .map { it to str.split(it).size }
            .toMap()
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
            } else if (event.isStartElement) {
                /**
                 * First Element ("Tag") found - store its offset
                 */
                val startElementEvent = (event as StartElement)

                val offset = startElementEvent.location.characterOffset

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
        var xmlRepresentation = c.resultPom.asXML()

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

            if (event.isStartElement) {
                /**
                 * Apply the formatting and tweak its XML Representation
                 */
                val startElementEvent = (event as StartElement)

                val offset = startElementEvent.location.characterOffset

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
    }
}