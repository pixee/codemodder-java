package io.openpixee.maven.operator.test

import `fun`.mike.dmp.DiffMatchPatch
import io.openpixee.maven.operator.POMOperator
import io.openpixee.maven.operator.ProjectModel
import io.openpixee.maven.operator.ProjectModelFactory
import org.dom4j.Document
import org.dom4j.io.SAXReader
import org.junit.Assert.assertFalse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.Diff
import java.io.File
import java.net.URLDecoder

open class AbstractTestBase {
    protected val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)

    /**
     * Implements a Given-When-Then idiom
     *
     * @param g: Given - returns a context
     * @param t: Then - validates given a context/ProjectModel
     */
    protected fun gwt(g: () -> ProjectModel, t: (p: ProjectModel) -> Unit) {
        val context = g()

        LOGGER.debug("context: {}", context)

        POMOperator.modify(context)

        LOGGER.debug("context after: {}", context)

        t(context)
    }

    protected fun gwt(name: String, pmf: ProjectModelFactory): ProjectModel =
        gwt(name, pmf.build())

    protected fun gwt(testName: String, context: ProjectModel): ProjectModel {
        val resultFile = "pom-$testName-result.xml"
        val resource = this.javaClass.javaClass.getResource(resultFile)

        if (resource != null) {
            val outcome = SAXReader().read(resource)

            LOGGER.debug("context: {}", context)

            POMOperator.modify(context)

            LOGGER.debug("context after: {}", context)

            assertFalse(
                "Expected and outcome have differences",
                getXmlDifferences(context.resultPom, outcome).hasDifferences()
            )
        } else {
            val resultFilePath = "src/test/resources/" + this.javaClass.`package`.name.replace(
                ".",
                "/"
            ) + "/" + resultFile

            LOGGER.debug("context: {}", context)

            POMOperator.modify(context)

            LOGGER.debug("context after: {}", context)

            LOGGER.warn("File $resultFilePath not found - writing results instead and ignorning assertions at all")

            File(resultFilePath).writeBytes(context.resultPomBytes)
        }

        return context
    }

    protected fun getXmlDifferences(
        original: Document,
        modified: Document
    ): Diff {
        val originalDoc = Input.fromString(original.asXML()).build()
        val modifiedDoc = Input.fromString(modified.asXML()).build()

        val diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc).ignoreWhitespace()
            .checkForSimilar().build()

        LOGGER.debug("diff: {}", diff)

        return diff
    }

    protected fun getTextDifferences(pomDocument: Document, resultPom: Document): Any {
        val pomDocumentAsString = pomDocument.asXML()
        val resultPomAsString = resultPom.asXML()

        val dmp = DiffMatchPatch()

        val diffs = dmp.patch_make(pomDocumentAsString, resultPomAsString)

        val patch = dmp.patch_toText(diffs)

        return URLDecoder.decode(patch, "utf-8")
    }
}
