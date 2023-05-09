package io.github.pixee.maven.operator

import org.apache.commons.io.output.NullOutputStream
import org.apache.maven.cli.MavenCli
import org.apache.maven.shared.invoker.InvocationRequest
import org.apache.maven.shared.invoker.MavenCommandLineBuilder
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

/**
 * Uses Maven Embedder to Implement
 */
class QueryByEmbedder : io.github.pixee.maven.operator.AbstractSimpleQueryCommand() {
    /**
     * Runs the "dependency:tree" mojo - but using Embedder instead.
     */
    override fun extractDependencyTree(outputPath: File, pomFilePath: File, c: ProjectModel) {
        val mavenCli = MavenCli()

        val cliBuilder = MavenCommandLineBuilder()

        val invocationRequest: InvocationRequest =
            buildInvocationRequest(outputPath, pomFilePath, c)

        val oldMultimoduleValue = System.getProperty(MAVEN_MULTIMODULE_PROJECT_DIRECTORY)

        System.setProperty(MAVEN_MULTIMODULE_PROJECT_DIRECTORY, pomFilePath.parent)

        try {
            val cliBuilderResult = cliBuilder.build(invocationRequest)

            val cliArgs = cliBuilderResult.commandline.toList().drop(1).toTypedArray()

            val baosOut =
                if (LOGGER.isDebugEnabled) ByteArrayOutputStream() else NullOutputStream.NULL_OUTPUT_STREAM

            val baosErr =
                if (LOGGER.isDebugEnabled) ByteArrayOutputStream() else NullOutputStream.NULL_OUTPUT_STREAM

            val result: Int = mavenCli.doMain(
                cliArgs,
                pomFilePath.parent,
                PrintStream(baosOut, true),
                PrintStream(baosErr, true)
            )

            if (LOGGER.isDebugEnabled) {
                LOGGER.debug("baosOut: {}", baosOut.toString())
                LOGGER.debug("baosErr: {}", baosErr.toString())
            }

            /**
             * Sometimes the Embedder will fail - it will return this specific exit code (1) as well as
             * not generate this file
             *
             * If that happens, we'll move to the next strategy (Invoker-based likely) by throwing a
             * custom exception which is caught inside the Chain#execute method
             *
             * @see Chain#execute
             */
            if (1 == result && (!outputPath.exists()))
                throw InvalidContextException()

            if (0 != result)
                throw IllegalStateException("Unexpected status code: %02d".format(result))
        } finally {
            if (null != oldMultimoduleValue)
                System.setProperty(MAVEN_MULTIMODULE_PROJECT_DIRECTORY, oldMultimoduleValue)
        }
    }

    companion object {
        const val MAVEN_MULTIMODULE_PROJECT_DIRECTORY = "maven.multiModuleProjectDirectory"

        private val LOGGER: Logger = LoggerFactory.getLogger(QueryByEmbedder::class.java)
    }
}