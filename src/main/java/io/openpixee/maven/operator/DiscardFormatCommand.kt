package io.openpixee.maven.operator

import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input

/**
 * Command Class to Short-Circuit/Discard Processing when no pom changes were made
 */
class DiscardFormatCommand : AbstractSimpleCommand() {
    override fun postProcess(c: ProjectModel): Boolean {
        val originalDoc = Input.fromString(String(c.originalPom)).build()
        val modifiedDoc = Input.fromString(c.resultPom.asXML()).build()

        val diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc)
            .ignoreWhitespace()
            .ignoreComments()
            .ignoreElementContentWhitespace()
            .checkForSimilar()
            .build()

        val hasDifferences = diff.hasDifferences()

        if (! (c.modifiedByCommand || hasDifferences)) {
            c.resultPomBytes = c.originalPom

            return true
        }

        return super.postProcess(c)
    }
}