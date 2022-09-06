package io.openpixee.maven.operator

import io.openpixee.diff.DiffMatchPatch
import io.openpixee.maven.operator.util.Util.buildLookupExpressionForDependency
import io.openpixee.maven.operator.util.Util.selectXPathNodes
import org.dom4j.Document
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.ComparisonType
import org.xmlunit.diff.Diff
import java.io.File
import java.net.URLDecoder

/**
 * Unit test for simple App.
 */
class POMOperatorTest {
    companion object {
        private val LOGGER : Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)
    }

    @Test
    fun testCaseOne() {
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.3")

        val context =
            ProjectModelFactory.load(
                POMOperator::class.java.getResource("pom-case-1.xml")!!,
                dependencyToUpgrade
            )

        LOGGER.debug("context: {}", context)

        POMOperator.upgradePom(context)

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())

        val textDiff = getTextDifferences(context.pomDocument, context.resultPom)

        LOGGER.debug("textDiff: {}", textDiff)

        assertThat("diff contains a <dependencyManagement> tag", textDiff.toString().contains("<dependencyManagement>"))
        assertThat("diff contains a <dependency> tag", textDiff.toString().contains("<dependency>"))

        val effectivePom = context.getEffectivePom()

        LOGGER.debug("effectivePom: {}", effectivePom.asXML())
    }

    private fun getTextDifferences(pomDocument: Document, resultPom: Document): Any {
        val pomDocumentAsString = pomDocument.asXML()
        val resultPomAsString = resultPom.asXML()

        val dmp = DiffMatchPatch()

        val diffs = dmp.patch_make(pomDocumentAsString, resultPomAsString)

        val patch = dmp.patch_toText(diffs)

        return URLDecoder.decode(patch, "utf-8")
    }

    @Test
    fun testCaseThree() {
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.3")

        val context =
            ProjectModelFactory.load(
                POMOperator::class.java.getResource("pom-case-3.xml")!!,
                dependencyToUpgrade
            )

        POMOperator.upgradePom(context)

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())
        assertThat("Document has a single difference", diff.differences.toList().size == 1)
        assertThat(
            "Document has different versions",
            diff.differences.toList()[0].comparison.type == ComparisonType.TEXT_VALUE
        )
        assertThat(
            "Document has changed version set to ${dependencyToUpgrade.version}",
            diff.differences.toList()[0].comparison.testDetails.value == dependencyToUpgrade.version
        )
    }

    @Test
    fun testCase4() {
        val pomPath = File(POMOperator::class.java.getResource("webgoat-parent.xml")!!.toURI())

        val exitCode = ProcessBuilder(
            "mvn",
            "-N",
            "install:install-file",
            "-DgroupId=org.owasp.webgoat",
            "-DartifactId=webgoat-parent",
            "-Dversion=8.2.3-SNAPSHOT",
            "-Dpackaging=pom",
            "-Dfile=${pomPath.absolutePath}"
        ).start().waitFor()

        assertThat("POM install was successful", 0 == exitCode)

        val dependencyToUpgrade =
            Dependency("org.apache.activemq", "activemq-amqp", version = "5.16.2")

        val context =
            ProjectModelFactory.load(
                POMOperator::class.java.getResource("pom-case-4.xml")!!,
                dependencyToUpgrade
            )

        POMOperator.upgradePom(context)

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())

        val effectivePom = context.getEffectivePom()

        assertThat(
            "Dependency has been changed",
            effectivePom.selectXPathNodes(buildLookupExpressionForDependency(dependencyToUpgrade))
                .isNotEmpty()
        )
    }

    private fun getXmlDifferences(
        original: Document,
        modified: Document
    ): Diff? {
        val originalDoc = Input.fromString(original.asXML()).build()
        val modifiedDoc = Input.fromString(modified.asXML()).build()

        val diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc).build()

        LOGGER.debug("diff: {}", diff)

        return diff
    }
}
