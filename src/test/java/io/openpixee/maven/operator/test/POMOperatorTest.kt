package io.openpixee.maven.operator.test

import io.openpixee.maven.operator.Dependency
import io.openpixee.maven.operator.MissingDependencyException
import io.openpixee.maven.operator.POMOperator
import io.openpixee.maven.operator.ProjectModelFactory
import io.openpixee.maven.operator.Util.buildLookupExpressionForDependency
import io.openpixee.maven.operator.Util.selectXPathNodes
import io.openpixee.maven.operator.Util.which
import org.apache.commons.lang3.SystemUtils
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.xmlunit.diff.ComparisonType
import java.io.File

/**
 * Unit test for simple App.
 */
class POMOperatorTest : AbstractTestBase() {
    @Test(expected = MissingDependencyException::class)
    fun testWithDependencyMissing() {
        gwt(
            "case-dependency-missing",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-1.xml")!!,
            )
        )
    }

    @Test
    fun testCaseOne() {
        val context = gwt(
            "case-1",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-1.xml")!!,
            ).withDependency(Dependency.fromString("org.dom4j:dom4j:2.0.3"))
        )

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

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

    @Test
    fun testCaseThree() {
        val dependencyToUpgradeOnCaseThree = Dependency("org.dom4j", "dom4j", version = "2.0.2")

        val context = gwt(
            "case-3",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-3.xml")!!,
            ).withDependency(dependencyToUpgradeOnCaseThree).withSkipIfNewer(false)
        )

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has differences", diff.hasDifferences())
        assertThat("Document has a single difference", diff.differences.toList().size == 1)
        assertThat(
            "Document has different versions",
            diff.differences.toList()[0].comparison.type == ComparisonType.TEXT_VALUE
        )
        assertThat(
            "Document has changed version set to ${dependencyToUpgradeOnCaseThree.version}",
            diff.differences.toList()[0].comparison.testDetails.value == dependencyToUpgradeOnCaseThree.version
        )
    }

    @Test
    fun testCaseThreeButWithLowerVersion() {
        val dependencyToUpgrade = Dependency("org.dom4j", "dom4j", version = "2.0.2")

        val context = gwt(
            "pom-case-three-with-lower-version",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-3.xml")!!,
            ).withDependency(dependencyToUpgrade).withSkipIfNewer(true)
        )

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has no differences", !diff.hasDifferences())
    }

    @Test
    fun testCase4() {
        val pomPath = File(POMOperatorTest::class.java.getResource("webgoat-parent.xml")!!.toURI())

        val args =
            if (SystemUtils.IS_OS_WINDOWS) {
                listOf("cmd.exe", "/c")
            } else {
                listOf()
            } +
                    listOf(
                        which("mvn")!!.absolutePath,
                        "-N",
                        "install:install-file",
                        "-DgroupId=org.owasp.webgoat",
                        "-DartifactId=webgoat-parent",
                        "-Dversion=8.2.3-SNAPSHOT",
                        "-Dpackaging=pom",
                        "-Dfile=${pomPath.absolutePath}"
                    )

        val exitCode = ProcessBuilder(
            *args.toTypedArray()
        ).start().waitFor()

        assertThat("POM install was successful", 0 == exitCode)

        val dependencyToUpgrade =
            Dependency("org.apache.activemq", "activemq-amqp", version = "5.16.2")

        val context = gwt(
            "case-4",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-case-4.xml")!!,
            ).withDependency(dependencyToUpgrade)
        )

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

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

        val context = gwt(
            "case-with-property",
            ProjectModelFactory.load(
                POMOperatorTest::class.java.getResource("pom-with-property-simple.xml")!!,
            ).withDependency(dependencyToUpgrade).withUseProperties(true).withSkipIfNewer(true)
        )

        LOGGER.debug("original pom: {}", context.pomDocument.asXML())
        LOGGER.debug("resulting pom: {}", context.resultPom.asXML())

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

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

    @Test(expected = IllegalStateException::class)
    fun testCaseWithPropertyDefinedTwice() {
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
    
    <properties>
      <dom4j.version>0.0.1</dom4j.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.dom4j</groupId>
            <artifactId>dom4j</artifactId>
            <version>${'$'}{dom4j.version}</version>
        </dependency>
        <dependency>
            <groupId>org.dom4j</groupId>
            <artifactId>dom4j-other</artifactId>
            <version>${'$'}{dom4j.version}</version>
        </dependency>
    </dependencies>
</project>
                """.trimIndent()
        val context =
            ProjectModelFactory.load(
                originalPom.byteInputStream(),
            ).withDependency(dependencyToUpgrade).withUseProperties(true)
                .withOverrideIfAlreadyExists(false)
                .build()

        POMOperator.modify(context)
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

        val diff = getXmlDifferences(context.pomDocument, context.resultPom)

        assertThat("Document has differences", diff.hasDifferences())

        val differencesAsList = diff.differences.toList()

        assertThat("Document has several differences", differencesAsList.size > 1)
    }

}
