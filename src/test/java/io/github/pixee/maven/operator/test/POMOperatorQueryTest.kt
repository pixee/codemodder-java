package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.*
import junit.framework.TestCase.*
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files

class POMOperatorQueryTest {
    private val LOGGER: Logger = LoggerFactory.getLogger(POMOperatorTest::class.java)

    @Test
    fun testBasicQuery() {
        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val context =
                ProjectModelFactory
                    .load(this.javaClass.getResource("pom-1.xml")!!)
                    .withQueryType(queryType)
                    .build()

            val dependencies = POMOperator.queryDependency(context)

            LOGGER.debug("Dependencies found: {}", dependencies)

            assertTrue("Dependencies are not empty", dependencies.isNotEmpty())
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

        assertTrue("Dependencies are empty", dependencies.isEmpty())
    }

    @Test(expected = IllegalStateException::class)
    fun testFailedUnsafeQuery() {
        val context =
            ProjectModelFactory
                .load(this.javaClass.getResource("pom-broken.xml")!!)
                .withQueryType(QueryType.UNSAFE)
                .build()

        val dependencies = POMOperator.queryDependency(context)

        assertTrue("Dependencies are empty", dependencies.isEmpty())
    }

    @Test
    fun testAllQueryTypes() {
        listOf("pom-1.xml", "pom-3.xml").forEach { pomFile ->
            Chain.AVAILABLE_QUERY_COMMANDS.forEach {
                val commandClassName = "io.github.pixee.maven.operator.${it.second}"

                val commandListOverride =
                    listOf(Class.forName(commandClassName).newInstance() as Command)

                val context =
                    ProjectModelFactory
                        .load(this.javaClass.getResource(pomFile)!!)
                        .withQueryType(QueryType.UNSAFE)
                        .build()

                val dependencies =
                    POMOperator.queryDependency(context, commandList = commandListOverride)

                assertTrue("Dependencies are not empty", dependencies.isNotEmpty())
            }
        }
    }


    @Test
    fun testTemporaryDirectory() {
        QueryType.values().filterNot { it == QueryType.NONE }.forEach { queryType ->
            val tempDirectory = File("/tmp/mvn-repo-" + System.currentTimeMillis() + ".dir")

            LOGGER.info("Using queryType: $queryType at $tempDirectory")

            assertFalse("Temp Directory does not exist initially", tempDirectory.exists())
            assertEquals(
                "There must be no files",
                tempDirectory.list()?.filter { File(it).isDirectory }?.size ?: 0,
                0,
            )

            val context =
                ProjectModelFactory
                    .load(this.javaClass.getResource("pom-1.xml")!!)
                    .withQueryType(queryType)
                    .withRepositoryPath(tempDirectory)
                    .build()

            val dependencies = POMOperator.queryDependency(context)

            LOGGER.debug("Dependencies found: {}", dependencies)

            assertTrue("Dependencies are not empty", dependencies.isNotEmpty())

            assertTrue("Temp Directory ends up existing", tempDirectory.exists())
            assertTrue("Temp Directory is a directory", tempDirectory.isDirectory)
            assertEquals(
                "There must be files",
                tempDirectory.list().filter { File(it).isDirectory }.size,
                0
            )
        }
    }
}