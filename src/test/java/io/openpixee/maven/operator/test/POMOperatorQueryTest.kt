package io.openpixee.maven.operator.test

import io.openpixee.maven.operator.*
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.assertTrue

class POMOperatorQueryTest {
    private val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)

    @Test
    fun testBasicQuery() {
        QueryType.values().forEach { queryType ->
            val context =
                ProjectModelFactory
                    .load(this.javaClass.getResource("pom-1.xml")!!)
                    .withQueryType(queryType)
                    .build()

            val dependencies = POMOperator.queryDependency(context)

            LOGGER.debug("Dependencies found: {}", dependencies)

            assertTrue(dependencies.isNotEmpty(), "Dependencies are not empty")
        }
    }

    @Test
    fun testFailedSafeQuery() {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource("pom-broken.xml")!!)
                .withQueryType(QueryType.SAFE)
                .build()

        val dependencies = POMOperator.queryDependency(context)

        assertTrue(dependencies.isEmpty(), "Dependencies are empty")
    }

    @Test(expected = IllegalStateException::class)
    fun testFailedUnsafeQuery() {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource("pom-broken.xml")!!)
                .withQueryType(QueryType.UNSAFE)
                .build()

        val dependencies = POMOperator.queryDependency(context)

        assertTrue(dependencies.isEmpty(), "Dependencies are empty")
    }

    @Test
    fun testAllQueryTypes() {
        listOf("pom-1.xml", "pom-3.xml").forEach { pomFile ->
            Chain.AVAILABLE_QUERY_COMMANDS.forEach {
                val commandClassName = "io.openpixee.maven.operator.${it.second}"

                val commandListOverride = listOf(Class.forName(commandClassName).newInstance() as Command)

                val context =
                    ProjectModelFactory
                        .load(this.javaClass.getResource(pomFile)!!)
                        .withQueryType(QueryType.UNSAFE)
                        .build()

                val dependencies = POMOperator.queryDependency(context, commandList = commandListOverride)

                assertTrue(dependencies.isNotEmpty(), "Dependencies are not empty")
            }
        }
    }
}