package br.com.ingenieux.pom.operator

import br.com.ingenieux.pom.operator.util.Util.buildLookupExpressionForDependency
import br.com.ingenieux.pom.operator.util.Util.selectXPathNodes
import org.dom4j.Document
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.xmlunit.builder.DiffBuilder
import org.xmlunit.builder.Input
import org.xmlunit.diff.ComparisonType
import org.xmlunit.diff.Diff
import java.io.File

/**
 * Unit test for simple App.
 */
class POMOperatorTest {
    @Test
    fun testCaseOne() {
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.3")

        val context =
            Context.load(POMOperator::class.java.getResource("pom-case-1.xml")!!, dependencyToUpgrade)

        POMOperator.upgradePom(context)

        val diff = getDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())
        assertThat("Document has three differences", diff.differences.toList().size == 3)

        val effectivePom = context.getEffectivePom()
    }

    @Test
    fun testCaseThree() {
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.3")

        val context =
            Context.load(POMOperator::class.java.getResource("pom-case-3.xml")!!, dependencyToUpgrade)

        POMOperator.upgradePom(context)

        val diff = getDifferences(context.pomDocument, context.resultPom)!!

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

        assertThat("POM install was successful", 0 == exitCode )

        val dependencyToUpgrade =
            Dependency("org.apache.activemq", "activemq-amqp", version = "5.16.2")

        val context =
            Context.load(POMOperator::class.java.getResource("pom-case-4.xml")!!, dependencyToUpgrade)

        POMOperator.upgradePom(context)

        val diff = getDifferences(context.pomDocument, context.resultPom)!!

        assertThat("Document has differences", diff.hasDifferences())

        val effectivePom = context.getEffectivePom()

        assertThat("Dependency has been changed", effectivePom.selectXPathNodes(buildLookupExpressionForDependency(dependencyToUpgrade)).isNotEmpty())
    }

    fun getDifferences(
        original: Document,
        modified: Document
    ): Diff? {
        val originalDoc = Input.fromString(original.asXML()).build()
        val modifiedDoc = Input.fromString(modified.asXML()).build()

        val diff = DiffBuilder.compare(originalDoc).withTest(modifiedDoc).build()
        return diff
    }

}