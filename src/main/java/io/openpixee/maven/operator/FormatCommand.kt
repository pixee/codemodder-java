package io.openpixee.maven.operator

import org.dom4j.io.OutputFormat
import org.dom4j.io.SAXWriter
import org.dom4j.io.XMLWriter
import java.lang.IllegalStateException
import java.nio.charset.Charset
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.StartDocument
import javax.xml.stream.events.StartElement

/**
 * This Command handles Formatting - particularly storing the original document preamble (the Processing Instruction and the first XML Element contents),
 * which are the only ones which are tricky to format (due to element and its attributes being freeform - thus formatting lost when serializing the DOM
 * and the PI being completely optional for the POM Document)
 */
class FormatCommand : AbstractSimpleCommand() {
    /**
     * Since the PI is Optional, we can actually leverage and store its charset to facilitate rendering when needed
     */
    private var charset: Charset = Charset.defaultCharset()

    /**
     * Preamble Contents are stored here
     */
    private var preamble: String = ""

    override fun execute(c: ProjectModel): Boolean {
        /**
         * Performs a StAX Parsing to Grab the first element
         */
        val inputFactory = XMLInputFactory.newInstance()
        val eventReader = inputFactory.createXMLEventReader(c.originalPom.inputStream())

        while (true) {
            val event = eventReader.nextEvent()

            if (event.isStartDocument && (event as StartDocument).encodingSet()) {
                /**
                 * Processing Instruction Found - Store its Character Encoding
                 */
                this.charset = Charset.forName((event as StartDocument).characterEncodingScheme)
            } else if (event.isStartElement) {
                /**
                 * First Element ("Tag") found - store its offset
                 */
                val startElementEvent = (event as StartElement)

                var offset = startElementEvent.location.characterOffset

                preamble = c.originalPom.toString(this.charset).substring(0, offset)

                break
            }

            if (! eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        return super.execute(c)
    }

    /**
     * When doing the opposite, render the XML using the optionally supplied encoding (defaults to UTF8 obviously)
     * but apply the original formatting as well
     */
    override fun postProcess(c: ProjectModel): Boolean {
        val writer = SAXWriter()

        var xmlRepresentation = c.resultPom.asXML()

        /**
         * We might need to replace the beginning of the POM with the same content
         * from the very beginning
         *
         * Grab the same initial offset from the formatted element like we did
         */
        val inputFactory = XMLInputFactory.newInstance()
        val eventReader = inputFactory.createXMLEventReader(xmlRepresentation.toByteArray(this.charset).inputStream())

        while (true) {
            val event = eventReader.nextEvent()

            if (event.isStartElement) {
                /**
                 * Apply the formatting and tweak its XML Representation
                 */
                val startElementEvent = (event as StartElement)

                var offset = startElementEvent.location.characterOffset

                xmlRepresentation = this.preamble + xmlRepresentation.substring(offset)

                break
            }

            /**
             * This code shouldn't be unreachable at all
             */
            if (! eventReader.hasNext())
                throw IllegalStateException("Couldn't find document start")
        }

        /**
         * Serializes it back
         */
        c.resultPomBytes = xmlRepresentation.toByteArray(this.charset)

        return super.postProcess(c)
    }
}