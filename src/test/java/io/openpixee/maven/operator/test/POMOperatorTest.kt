package io.openpixee.maven.operator.test

import `fun`.mike.dmp.DiffMatchPatch
import io.openpixee.maven.operator.Dependency
import io.openpixee.maven.operator.POMOperator
import io.openpixee.maven.operator.ProjectModelFactory
import io.openpixee.maven.operator.util.Util.buildLookupExpressionForDependency
import io.openpixee.maven.operator.util.Util.selectXPathNodes
import org.dom4j.Document
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
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
        private val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)
    }

    @Test
    fun testCaseOne() {
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.3")

        val context =
            ProjectModelFactory.load(
                POMOperator::class.java.getResource("pom-case-1.xml")!!,
            ).withDependency(dependencyToUpgrade).build()

        LOGGER.debug("context: {}", context)

        POMOperator.modify(context)

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())

        val textDiff = getTextDifferences(context.pomDocument, context.resultPom)

        LOGGER.debug("textDiff: {}", textDiff)

        assertThat(
            "diff contains a <dependencyManagement> tag",
            textDiff.toString().contains("<dependencyManagement>")
        )
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
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.2")

        val context =
            ProjectModelFactory.load(
                POMOperator::class.java.getResource("pom-case-3.xml")!!,
            ).withDependency(dependencyToUpgrade).withSkipIfNewer(false).build()


        POMOperator.modify(context)

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
    fun testCaseThreeButWithLowerVersion() {
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.2")

        val context =
            ProjectModelFactory.load(
                POMOperator::class.java.getResource("pom-case-3.xml")!!,
            ).withDependency(dependencyToUpgrade).withSkipIfNewer(true).build()


        POMOperator.modify(context)

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has no differences", !diff.hasDifferences())
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
            ).withDependency(dependencyToUpgrade).build()

        POMOperator.modify(context)

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())

        val effectivePom = context.getEffectivePom()

        assertThat(
            "Dependency has been changed",
            effectivePom.selectXPathNodes(buildLookupExpressionForDependency(dependencyToUpgrade))
                .isNotEmpty()
        )
    }

    @Test
    fun testCaseWithProperty() {
        val dependencyToUpgrade =
            Dependency("org.dom4j", "dom4j", version = "1.0.0")

        val context =
            ProjectModelFactory.load(
                POMOperator::class.java.getResource("pom-with-property-simple.xml")!!,
            ).withDependency(dependencyToUpgrade).withUseProperties(true).withSkipIfNewer(true)
                .build()

        POMOperator.modify(context)

        LOGGER.debug("original pom: {}", context.pomDocument.asXML())
        LOGGER.debug("resulting pom: {}", context.resultPom.asXML())

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())

        val differenceList = diff.differences.toList()

        assertThat("Document has one difference", 1 == differenceList.size)

        assertThat(
            "Document changes a single version",
            differenceList.first().toString()
                .startsWith("Expected text value '0.0.1-SNAPSHOT' but was '1.0.0'")
        )

        assertEquals(
            "Document changes a property called 'sample.version'",
            differenceList.first().comparison.testDetails.xPath,
            "/project[1]/properties[1]/sample.version[1]/text()[1]"
        )
    }

    @Test
    fun testCaseWithoutPropertyButDefiningOne() {
        val dependencyToUpgrade =
            Dependency("org.dom4j", "dom4j", version = "1.0.0")

        val originalPom = """
<?xml version="1.0" encoding="UTF-8"?>
<project
        xmlns="http://maven.apache.org/POM/4.0.0"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>br.com.ingenieux</groupId>
    <artifactId>pom-operator</artifactId>
    <version>0.0.1-SNAPSHOT</version>

    <dependencies>
        <dependency>
            <groupId>org.dom4j</groupId>
            <artifactId>dom4j</artifactId>
            <version>0.0.1-SNAPSHOT</version>
        </dependency>
    </dependencies>
</project>
                """.trim()
        val context =
            ProjectModelFactory.load(
                originalPom.byteInputStream(),
            ).withDependency(dependencyToUpgrade).withUseProperties(true).withSkipIfNewer(true)
                .build()

        POMOperator.modify(context)

        LOGGER.debug("original pom: {}", context.pomDocument.asXML())
        LOGGER.debug("resulting pom: {}", context.resultPom.asXML())

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())

        val differencesAsList = diff.differences.toList()

        assertThat("Document has several differences", differencesAsList.size > 1)
    }

    private fun getXmlDifferences(
        original: Document,
        modified: Document
    ): Diff? {
        val originalDoc = Input.fromString(original.asXML()).build()
        val modifiedDoc = Input.fromString(modified.asXML()).build()

        val diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc).ignoreWhitespace()
            .checkForSimilar().build()

        LOGGER.debug("diff: {}", diff)

        return diff
    }
}
