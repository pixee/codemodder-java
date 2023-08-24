package io.github.pixee.maven.operator

import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input

/**
 * Command Class to Short-Circuit/Discard Processing when no pom changes were made
 */
class DiscardFormatCommand : AbstractCommand() {
    override fun postProcess(pm: ProjectModel): Boolean {
        var mustSkip = false

        for (pomFile in pm.allPomFiles) {
            val originalDoc = Input.fromString(String(pomFile.originalPom)).build()
            val modifiedDoc = Input.fromString(pomFile.resultPom.asXML()).build()

            val diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc)
                .ignoreWhitespace()
                .ignoreComments()
                .ignoreElementContentWhitespace()
                .checkForSimilar()
                .build()

            val hasDifferences = diff.hasDifferences()

            if (!(pm.modifiedByCommand || hasDifferences)) {
                pomFile.resultPomBytes = pomFile.originalPom

                mustSkip = true
            }
        }

        /**
         * Triggers early abandonment
         */
        if (mustSkip)
            return true

        return super.postProcess(pm)
    }
}