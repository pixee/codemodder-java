package io.github.pixee.maven.operator

import io.github.pixee.maven.operator.Util.which
import org.apache.commons.lang3.SystemUtils
import org.apache.maven.shared.invoker.DefaultInvocationRequest
import org.apache.maven.shared.invoker.InvocationRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Paths
import java.util.*

/**
 * Common Base Class - Meant to be used by Simple Queries using either Invoker and/or Embedder, thus
 * relying on dependency:tree mojo outputting into a text file - which might be cached.
 *
 */
abstract class AbstractQueryCommand : AbstractCommand() {
    /**
     * Generates a temporary file path used to store the output of the <pre>dependency:tree</pre> mojo
     *
     * @param pomFilePath POM Original File Path
     */
    private fun getOutputPath(pomFilePath: File): File {
        val basePath = pomFilePath.parentFile

        val outputBasename = "output-%08X.txt".format(pomFilePath.hashCode())

        val outputPath = File(basePath, outputBasename)

        return outputPath
    }

    /**
     * Given a POM URI, returns a File Object
     *
     * @param d POMDocument
     */
    protected fun getPomFilePath(d: POMDocument): File = Paths.get(d.pomPath!!.toURI()).toFile()

    /**
     * Abstract Method to extract dependencies
     *
     * @param outputPath Output Path to where to store the content
     * @param pomFilePath Input Pom Path
     * @param c Project Model
     */
    abstract fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel)

    /**
     * Internal Holder Variable
     *
     * Todo: OF COURSE IT BREAKS THE PROTOCOL
     */
    internal var result: Collection<Dependency>? = null

    /**
     * We declare the main logic here - details are made in the child classes for now
     */

    override fun execute(pm: ProjectModel): Boolean {
        val pomFilePath = getPomFilePath(pm.pomFile)

        val outputPath = getOutputPath(pomFilePath)

        if (outputPath.exists()) {
            outputPath.delete()
        }

        try {
            extractDependencyTree(outputPath, pomFilePath, pm)
        } catch (e: InvalidContextException) {
            return false
        }

        this.result = extractDependencies(outputPath).values

        return true
    }

    /**
     * Given a File containing the output of the dependency:tree mojo, read its contents and parse, creating an array of dependencies
     *
     * About the file contents: We receive something such as this, then filter it out:
     *
     * <pre>
     *     br.com.ingenieux:pom-operator:jar:0.0.1-SNAPSHOT
     *     +- xerces:xercesImpl:jar:2.12.1:compile
     *     |  \- xml-apis:xml-apis:jar:1.4.01:compile
     *     \- org.jetbrains.kotlin:kotlin-test:jar:1.5.31:test
     * </pre>
     *
     * @param outputPath file to read
     */
    protected fun extractDependencies(outputPath: File) = outputPath.readLines().drop(1).map {
        it.trim(*"+-|\\ ".toCharArray())
    }.map {
        it to it.split(':')
    }.associate { (line, elements) ->
        val (groupId, artifactId, packaging, version, scope) = elements

        line to Dependency(
            groupId = groupId,
            artifactId = artifactId,
            version = version,
            packaging = packaging,
            scope = scope
        )
    }

    protected fun buildInvocationRequest(
        outputPath: File,
        pomFilePath: File,
        c: ProjectModel
    ): InvocationRequest {
        val props = Properties(System.getProperties()).apply {
            setProperty("outputFile", outputPath.absolutePath)

            val localRepositoryPath = getLocalRepositoryPath(c).absolutePath

            setProperty("maven.repo.local", localRepositoryPath)
        }

        val request: InvocationRequest = DefaultInvocationRequest().apply {
            findMaven(this)

            pomFile = pomFilePath

            isShellEnvironmentInherited = true

            isNoTransferProgress = true
            isBatchMode = true
            isRecursive = false
            profiles = c.activeProfiles.toList()
            isDebug = true

            isOffline = c.offline

            properties = props

            goals = listOf(DEPENDENCY_TREE_MOJO_REFERENCE)
        }

        return request
    }

    /**
     * Locates where Maven is at - HOME var and main launcher script.
     *
     * @param invocationRequest InvocationRequest to be filled up
     */
    private fun findMaven(invocationRequest: InvocationRequest) {
        /*
         * Step 1: Locate Maven Home
         */
        val m2homeEnvVar = System.getenv("M2_HOME")

        if (null != m2homeEnvVar) {
            val m2HomeDir = File(m2homeEnvVar)

            if (m2HomeDir.isDirectory)
                invocationRequest.mavenHome = m2HomeDir
        }

        /**
         * Step 1.1: Try to guess if thats the case
         */
        if (invocationRequest.mavenHome == null) {
            val inferredHome = File(SystemUtils.getUserHome(), ".m2")

            if (!(inferredHome.exists() && inferredHome.isDirectory)) {
                LOGGER.warn(
                    "Inferred User Home - which does not exist or not a directory: {}",
                    inferredHome
                )
            }

            invocationRequest.mavenHome = inferredHome
        }

        /**
         * Step 2: Find Maven Executable given the operating system and PATH variable contents
         */
        val foundExecutable = listOf("mvn", "mvnw").map { which(it) }.firstOrNull()

        if (null != foundExecutable) {
            invocationRequest.mavenExecutable = foundExecutable

            return
        }

        throw IllegalStateException("Missing Maven Home / Executable")
    }

    companion object {
        /**
         * Mojo Reference
         */
        const val DEPENDENCY_TREE_MOJO_REFERENCE =
            "org.apache.maven.plugins:maven-dependency-plugin:3.3.0:tree"

        val LOGGER: Logger = LoggerFactory.getLogger(AbstractQueryCommand::class.java)
    }

    override fun postProcess(c: ProjectModel): Boolean = false
}
