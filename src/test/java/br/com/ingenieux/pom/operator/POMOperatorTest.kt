package br.com.ingenieux.pom.operator

import org.dom4j.Document
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.xmlunit.diff.ComparisonType

/**
 * Unit test for simple App.
 */
class POMOperatorTest : BaseTest() {
    @Test
    fun testSimplePOMUpgrade() {
        val pomDocument = POMOperator.readResourcePom("pom-1.xml")

        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.3")

        pomUpgradeAndAssert(pomDocument, dependencyToUpgrade)
    }

    @Test
    fun testComplexPOMUpgrade() {
        val pomDocument = POMOperator.readResourcePom("pom-2.xml", effectivePom = true)

        val dependencyToUpgrade = Dependency("junit", "junit", version = "4.13")

        pomUpgradeAndAssert(pomDocument, dependencyToUpgrade)
    }

    @Test(expected = IllegalStateException::class)
    fun testMissingPomChange() {
        val pomDocument = POMOperator.readResourcePom("pom-2.xml")

        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.3")

        POMOperator.upgradePom(pomDocument, dependencyToUpgrade)
    }

    private fun pomUpgradeAndAssert(
        pomDocument: Document,
        dependencyToUpgrade: Dependency
    ) {
        val changedPom = POMOperator.upgradePom(pomDocument, dependencyToUpgrade)

        println(changedPom.asXML())

        val diff = getDifferences(pomDocument, changedPom)

        val oDiff = diff!!

        assertThat("Document has differences", oDiff.hasDifferences())
        assertThat("Document has a single difference", oDiff.differences.toList().size == 1)
        assertThat(
            "Document has different versions",
            oDiff.differences.toList()[0].comparison.type == ComparisonType.TEXT_VALUE
        )
        assertThat(
            "Document has changed version set to ${dependencyToUpgrade.version}",
            oDiff.differences.toList()[0].comparison.testDetails.value == dependencyToUpgrade.version
        )
    }
}