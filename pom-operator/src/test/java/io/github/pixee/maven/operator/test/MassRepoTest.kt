package io.github.pixee.maven.operator.test

import io.github.pixee.maven.operator.Dependency
import io.github.pixee.maven.operator.POMOperator
import io.github.pixee.maven.operator.POMScanner
import io.github.pixee.maven.operator.ProjectModelFactory
import io.github.pixee.maven.operator.Util.which
import org.apache.commons.lang3.SystemUtils
import org.junit.Assert.*
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.lang.AssertionError
import java.lang.Exception

data class TestRepo(
    val slug: String,
    val branch: String = "master",
    val pomPath: String = "pom.xml",
    val useProperties: Boolean = false,
    val useScanner: Boolean = false,
    val offline: Boolean = false,
    val commitId: String? = null,
) {
    fun cacheDir() = BASE_CACHE_DIR.resolve("repo-%08X".format(slug.hashCode()))

    companion object {
        val BASE_CACHE_DIR: File = File(System.getProperty("user.dir") + "/.cache").absoluteFile
    }
}

/**
 * This test actually picks up several github repos and apply the POM Operator and checks for a runtime dependency change
 */
class MassRepoIT {
    /*
    https://github.com/trending/java?since=daily

    apache/pulsar
    metersphere/metersphere
    apache/rocketmq
    OpenAPITools/openapi-generator
    casbin/jcasbin
    trinodb/trino
    bytedeco/javacv
    flowable/flowable-engine

    Azure/azure-sdk-for-java
    questdb/questdb
     */

    private val repos = listOf(
        TestRepo(
            "WebGoat/WebGoat",
            useProperties = true,
            branch = "main",
            useScanner = false,
            commitId = "e75cfbeb110e3d3a2ca3c8fee2754992d89c419d",
            pomPath = "webgoat-lessons/xxe/pom.xml",
        ) to "io.github.pixee:java-security-toolkit:1.0.2",
        TestRepo(
            "WebGoat/WebGoat",
            useProperties = true,
            branch = "main",
            useScanner = true,
            offline = true,
            pomPath = "webgoat-container/pom.xml"
        ) to "io.github.pixee:java-security-toolkit:1.0.2",
        TestRepo(
            "WebGoat/WebGoat",
            useProperties = true,
            branch = "main",
            useScanner = false,
            pomPath = "webgoat-container/pom.xml"
        ) to "io.github.pixee:java-security-toolkit:1.0.2",
        TestRepo(
            "WebGoat/WebGoat",
            useProperties = true,
            branch = "main",
            pomPath = "webgoat-container/pom.xml"
        ) to "io.github.pixee:java-security-toolkit:1.0.2",
        TestRepo(
            "CRRogo/vert.x",
            useProperties = true,
        ) to "io.github.pixee:java-security-toolkit:1.0.2",
        TestRepo(
            "apache/pulsar",
            pomPath = "pulsar-broker/pom.xml"
        ) to "commons-codec:commons-codec:1.14",
        TestRepo(
            "apache/rocketmq",
            pomPath = "common/pom.xml"
        ) to "commons-codec:commons-codec:1.15",
        TestRepo(
            "OpenAPITools/openapi-generator",
            pomPath = "modules/openapi-generator-core/pom.xml"
        ) to "com.google.guava:guava:31.0-jre",
        TestRepo(
            "casbin/jcasbin",
        ) to "com.google.code.gson:gson:2.8.0",
        TestRepo(
            "bytedeco/javacv"
        ) to "org.jogamp.jocl:jocl-main:2.3.1",
    )

    /**
     * Checks out - or resets - a stored github repo
     */
    private fun checkoutOrResetCachedRepo(repo: TestRepo) {
        LOGGER.info("Checkout out $repo into ${repo.cacheDir()}")

        if (!repo.cacheDir().exists()) {
            // git clone -b branch github.com/slug/ dir
            val command = arrayOf(
                "git",
                "clone",
                "-b",
                repo.branch,
                "https://github.com/${repo.slug}.git",
                repo.cacheDir().canonicalPath
            )

            LOGGER.debug("Running command: ${command.joinToString(" ")}")

            val process = ProcessBuilder(*command)
                .directory(TestRepo.BASE_CACHE_DIR.canonicalFile)
                .inheritIO()
                .start()

            process.waitFor()
        } else {
            val command = arrayOf("git", "reset", "--hard", "HEAD")

            LOGGER.debug("Running command: ${command.joinToString(" ")}")

            val process = ProcessBuilder(*command)
                .directory(repo.cacheDir())
                .inheritIO()
                .start()

            process.waitFor()
        }

        if (null != repo.commitId) {
            val command = arrayOf("git", "checkout", repo.commitId)

            LOGGER.debug("Running command: ${command.joinToString(" ")}")

            val process = ProcessBuilder(*command)
                .directory(repo.cacheDir())
                .inheritIO()
                .start()

            process.waitFor()
        }
    }

    /**
     * Sanity Test on a single repo
     */
    @Test
    fun testBasic() {
        val firstCase = repos.first()

        testOnRepo(firstCase.first, firstCase.second)
    }

    /**
     * THATS THE FULL TEST
     */
    @Test
    fun testAllOthers() {
        repos.forEachIndexed { n, pair ->
            try {
                testOnRepo(pair.first, pair.second)
            } catch (e: Throwable) {
                throw AssertionError("while trying example $n of $pair", e)
            }
        }
    }

    private fun testOnRepo(
        sampleRepo: TestRepo,
        dependencyToUpgradeString: String
    ) {
        LOGGER.info(
            "Testing on repo {}, branch {} with dependency {} ({})",
            sampleRepo.slug,
            sampleRepo.branch,
            dependencyToUpgradeString,
            sampleRepo,
        )

        checkoutOrResetCachedRepo(sampleRepo)

        val originalDependencies = getDependenciesFrom(sampleRepo)

        LOGGER.info("dependencies: {}", originalDependencies)

        val dependencyToUpgrade = Dependency.fromString(dependencyToUpgradeString)

        val projectModelFactory = if (sampleRepo.useScanner) {
            POMScanner.scanFrom(
                File(sampleRepo.cacheDir(), sampleRepo.pomPath),
                sampleRepo.cacheDir()
            )
        } else {
            ProjectModelFactory.Companion.load(File(sampleRepo.cacheDir(), sampleRepo.pomPath))
        }

        val context = projectModelFactory
            .withDependency(dependencyToUpgrade)
            .withSkipIfNewer(false)
            .withUseProperties(sampleRepo.useProperties)
            .withOffline(sampleRepo.offline)
            .build()

        val result = POMOperator.modify(context)

        context.allPomFiles.filter { it.dirty }.forEach {
            it.file.writeBytes(it.resultPomBytes)
        }

        val finalDependencies =
            getDependenciesFrom(sampleRepo)

        LOGGER.info("dependencies: {}", finalDependencies)

        val queryFailed = "" == originalDependencies && "" == finalDependencies

        if (queryFailed) {
            assertTrue("Must be modified even when query failed", result)
        } else {
            val dependencyAsStringWithPackaging = dependencyToUpgrade.toString()

            assertFalse(
                "Dependency should be originally missing", originalDependencies.contains(
                    dependencyAsStringWithPackaging
                )
            )
            assertTrue(
                "New Dependency should be appearing", finalDependencies.contains(
                    dependencyAsStringWithPackaging
                )
            )
        }
    }

    private fun getDependenciesFrom(repo: TestRepo): String = try {
        getDependenciesFrom(repo.pomPath, repo.cacheDir())
    } catch (e: Exception) {
        val pomFile = File(repo.cacheDir(), repo.pomPath)

        val dependencies =
            POMOperator.queryDependency(
                POMScanner.scanFrom(pomFile, repo.cacheDir())
                    .withRepositoryPath(repo.cacheDir())
                    .withOffline(false)
                    .build()
            )

        dependencies.joinToString("\n")
    }

    private fun getDependenciesFrom(pomPath: String, dir: File): String {
        val outputFile = File.createTempFile("tmp-pom", ".txt")

        if (outputFile.exists()) {
            outputFile.delete()
        }

        val command = if (SystemUtils.IS_OS_WINDOWS) {
            listOf(which("cmd")!!.canonicalPath, "/c")
        } else {
            emptyList()
        } + listOf(
            which("mvn")!!.canonicalPath,
            "-B",
            "-f",
            pomPath,
            "dependency:tree",
            "-Dscope=runtime",
            "-DoutputFile=${outputFile.canonicalPath}"
        )

        LOGGER.debug("Running: " + command.joinToString(" "))
        LOGGER.debug("Dir: " + dir)

        val process = ProcessBuilder(*command.toTypedArray())
            .directory(dir)
            .inheritIO()
            .start()

        process.waitFor()

        if (! outputFile.exists())
            return ""

        val result = outputFile.readText()

        outputFile.delete()

        return result
    }

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(MassRepoIT::class.java)
    }

    init {
        /**
         * Creates the Cache Directory
         */
        if (!TestRepo.BASE_CACHE_DIR.exists())
            TestRepo.BASE_CACHE_DIR.mkdirs()
    }
}
