package io.openpixee.maven.operator

import org.dom4j.Document
import org.dom4j.io.SAXReader
import java.io.File
import java.io.FileInputStream

/**
 * ProjectModel represents the input parameters for the chain
 *
 * @todo Wrap it into a <pre>Context</pre> interface
 */
class ProjectModel internal constructor(
    val pomDocument: Document,
    val dependency: Dependency,
    val skipIfNewer: Boolean,
    val useProperties: Boolean,
    val activeProfiles: Set<String>
) {
    val resultPom: Document = pomDocument.clone() as Document

    internal val resolvedProperties: Map<String, String> by lazy {
        getEffectivePom().rootElement.elements("properties").flatMap { it.elements() }
            .associate {
                it.name to it.text
            }
    }

    fun getEffectivePom(): Document {
        val tmpInputFile = File.createTempFile("tmp-pom-orig", ".xml")

        tmpInputFile.writeText(resultPom.asXML())

        val tmpOutputFile = File.createTempFile("tmp-pom", ".xml")

        val processArgs = mutableListOf<String>(
            "mvn",
            "-B",
            "-N",
            "-f",
            tmpInputFile.absolutePath,
        )

        if (this.activeProfiles.isNotEmpty()) {
            // TODO Aldrin: How safe is not to escape those things? My concern is that deactivating a profile uses '!',
            //  and I'm not sure how shell escaping rules play a part on that
            processArgs.addAll(listOf("-P", this.activeProfiles.joinToString(",")))
        }

        processArgs.addAll(
            listOf(
                "help:effective-pom",
                "-Doutput=${tmpOutputFile.absolutePath}"
            )
        )

        val psBuilder = ProcessBuilder(processArgs).inheritIO()

        psBuilder.environment().putAll(System.getenv())

        val process = psBuilder.start()

        val retCode = process.waitFor()

        if (0 != retCode)
            throw IllegalStateException("Unexpected return code from maven: $retCode")

        return SAXReader().read(FileInputStream(tmpOutputFile))
    }
}

